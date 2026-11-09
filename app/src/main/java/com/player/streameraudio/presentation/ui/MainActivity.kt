package com.player.streameraudio.presentation.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.auth.AuthTabIntent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.player.streameraudio.BuildConfig
import com.player.streameraudio.R
import com.player.streameraudio.data.update.DirectUpdateResult
import com.player.streameraudio.data.update.GitHubUpdateChecker
import com.player.streameraudio.data.util.Constants
import com.player.streameraudio.presentation.update.AppUpdateUiState
import com.player.streameraudio.presentation.update.UpdateAction
import com.player.streameraudio.presentation.ui.theme.StreamerAudioTheme
import com.player.streameraudio.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    @Inject lateinit var githubUpdateChecker: GitHubUpdateChecker

    private lateinit var appUpdateManager: AppUpdateManager
    private var updateState by mutableStateOf<AppUpdateUiState>(AppUpdateUiState.Idle)
    private val playUpdateLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK && updateState !is AppUpdateUiState.ReadyToInstall) {
                updateState = AppUpdateUiState.Idle
            }
        }
    private val installStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val total = state.totalBytesToDownload()
                val percent = if (total > 0L) ((state.bytesDownloaded() * 100L) / total).toInt() else null
                updateState = AppUpdateUiState.Downloading(percent)
            }
            InstallStatus.DOWNLOADED -> updateState = AppUpdateUiState.ReadyToInstall
            InstallStatus.FAILED -> updateState = AppUpdateUiState.Error("The update download failed. Please try again.")
            else -> Unit
        }
    }
    private val authLauncher =
        AuthTabIntent.registerActivityResultLauncher(this) { result ->
            when (result.resultCode) {
                AuthTabIntent.RESULT_VERIFICATION_FAILED -> viewModel.reportAuthError(
                    getString(R.string.oauth_app_link_verification_failed)
                )
                AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT -> viewModel.reportAuthError(
                    getString(R.string.oauth_app_link_verification_timed_out)
                )
                else -> viewModel.handleAuthResult(result.resultUri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appUpdateManager = AppUpdateManagerFactory.create(this)
        setContent {
            StreamerAudioTheme {
                StreamerAudioApp(
                    viewModel = viewModel,
                    onLogin = ::launchTwitchLogin,
                    updateState = updateState,
                    onCheckForUpdates = { checkForUpdates(manual = true) },
                    onStartUpdate = ::startUpdate,
                    onDismissUpdate = ::dismissUpdatePrompt,
                    onCompleteUpdate = appUpdateManager::completeUpdate
                )
            }
        }
        handleCallbackIntent(intent)
        if (!BuildConfig.USE_DEMO_CONTENT) checkForUpdates(manual = false)
    }

    override fun onStart() {
        super.onStart()
        appUpdateManager.registerListener(installStateListener)
    }

    override fun onResume() {
        super.onResume()
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    updateState = AppUpdateUiState.ReadyToInstall
                }
            }
        }
    }

    override fun onStop() {
        if (::appUpdateManager.isInitialized) appUpdateManager.unregisterListener(installStateListener)
        super.onStop()
    }

    private fun checkForUpdates(manual: Boolean) {
        if (manual) updateState = AppUpdateUiState.Checking
        if (githubUpdateChecker.isInstalledFromGooglePlay()) {
            checkGooglePlayUpdate()
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                val result = runCatching { githubUpdateChecker.check() }
                withContext(Dispatchers.Main) {
                    updateState = result.fold(
                        onSuccess = { value ->
                            when (value) {
                                is DirectUpdateResult.Available -> AppUpdateUiState.Available(
                                    versionName = value.update.versionName,
                                    action = UpdateAction.DirectDownload(value.update.downloadUrl)
                                )
                                DirectUpdateResult.UpToDate -> AppUpdateUiState.UpToDate
                            }
                        },
                        onFailure = {
                            if (manual) AppUpdateUiState.Error("Couldn't check for updates. Check your connection and try again.")
                            else AppUpdateUiState.Idle
                        }
                    )
                }
            }
        }
    }

    private fun checkGooglePlayUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                updateState = if (
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    AppUpdateUiState.Available("available on Google Play", UpdateAction.GooglePlay)
                } else {
                    AppUpdateUiState.UpToDate
                }
            }
            .addOnFailureListener {
                updateState = AppUpdateUiState.Error("Google Play couldn't check for updates. Please try again.")
            }
    }

    private fun startUpdate(action: UpdateAction) {
        when (action) {
            is UpdateAction.DirectDownload -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(action.url)))
            UpdateAction.GooglePlay -> appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                runCatching {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        playUpdateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                    )
                }.onFailure {
                    updateState = AppUpdateUiState.Error("Couldn't start the Google Play update.")
                }
            }
        }
    }

    private fun dismissUpdatePrompt() {
        val available = updateState as? AppUpdateUiState.Available ?: return
        updateState = available.copy(prompt = false)
    }

    private fun launchTwitchLogin() {
        val redirectUri = Uri.parse(Constants.TWITCH_REDIRECT_URL)
        val redirectHost = requireNotNull(redirectUri.host) {
            "Twitch redirect URL must include an HTTPS host"
        }
        AuthTabIntent.Builder().build().launch(
            authLauncher,
            viewModel.createAuthUrl(),
            redirectHost,
            redirectUri.path.orEmpty()
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCallbackIntent(intent)
    }

    private fun handleCallbackIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        val expected = Uri.parse(Constants.TWITCH_REDIRECT_URL)
        if (uri.scheme == expected.scheme &&
            uri.host == expected.host &&
            uri.path.orEmpty().trimEnd('/') == expected.path.orEmpty().trimEnd('/')
        ) {
            viewModel.handleAuthResult(uri)
        }
    }
}
