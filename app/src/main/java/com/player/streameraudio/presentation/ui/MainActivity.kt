package com.player.streameraudio.presentation.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.auth.AuthTabIntent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.player.streameraudio.presentation.ui.theme.StreamerAudioTheme
import com.player.streameraudio.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val authLauncher =
        AuthTabIntent.registerActivityResultLauncher(this) { result -> viewModel.handleAuthResult(result.resultUri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StreamerAudioTheme {
                StreamerAudioApp(
                    viewModel = viewModel,
                    onLogin = {
                        AuthTabIntent.Builder().build().launch(
                            authLauncher,
                            viewModel.createAuthUrl(),
                            "streameraudio-twitch-backend.netlify.app",
                            "/oauth/callback"
                        )
                    }
                )
            }
        }
        handleCallbackIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCallbackIntent(intent)
    }

    private fun handleCallbackIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "https" &&
            uri.host == "streameraudio-twitch-backend.netlify.app" &&
            uri.path == "/oauth/callback"
        ) {
            viewModel.handleAuthResult(uri)
        }
    }
}
