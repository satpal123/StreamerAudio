package com.player.streameraudio.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.auth.AuthTabIntent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.player.streameraudio.R
import com.player.streameraudio.data.util.Constants
import com.player.streameraudio.presentation.ui.theme.StreamerAudioTheme
import com.player.streameraudio.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
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
        setContent {
            StreamerAudioTheme {
                StreamerAudioApp(
                    viewModel = viewModel,
                    onLogin = ::launchTwitchLogin
                )
            }
        }
        handleCallbackIntent(intent)
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
