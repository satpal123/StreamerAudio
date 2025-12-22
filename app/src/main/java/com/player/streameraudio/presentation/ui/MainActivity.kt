package com.player.streameraudio.presentation.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.player.streameraudio.R
import com.player.streameraudio.data.util.Constants
import com.player.streameraudio.data.util.ResponseState
import com.player.streameraudio.data.util.SecurePreferences
import com.player.streameraudio.presentation.viewmodel.TwitchAuthViewModel
import com.player.streameraudio.presentation.viewmodel.TwitchUserViewModel
import com.player.streameraudio.presentation.viewmodel.TwitchValidateTokenViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: TwitchAuthViewModel by viewModels()
    private val validateTokenViewModel: TwitchValidateTokenViewModel by viewModels()
    private val userViewModel: TwitchUserViewModel by viewModels()

    // Use encrypted SharedPreferences
    private val securePrefs: SharedPreferences by lazy {
        SecurePreferences.getEncryptedSharedPreferences(this)
    }

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var skipLoginButton: Button
    private lateinit var loginButton: MaterialButton
    private lateinit var loginPrompt: View

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        progressBar = findViewById(R.id.progressBar)
        skipLoginButton = findViewById(R.id.skipLoginButton)
        loginButton = findViewById(R.id.loginButton)
        loginPrompt = findViewById(R.id.loginPrompt)
        webView = findViewById(R.id.webView)

        // Ã¢Å“â€¦ FIX: Login button click listener
        loginButton.setOnClickListener {
            setupWebView()
        }

        // Skip login button - browse without authentication
        skipLoginButton.setOnClickListener {
            redirectToStreamersActivity(null)
        }

        // Get token and user ID from encrypted storage
        val accessToken = securePrefs.getString(Constants.PREF_ACCESS_TOKEN, null)
        val userId = securePrefs.getString(Constants.PREF_USER_ID, null)

        if (!accessToken.isNullOrEmpty() && !userId.isNullOrEmpty()) {
            // Hide login options
            loginPrompt.visibility = View.GONE

            handleLoading()
            validateAccessToken(accessToken)
        } else {
            // Show login options
            loginPrompt.visibility = View.VISIBLE
            webView.visibility = View.GONE
        }
    }

    private fun validateAccessToken(accessToken: String) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                validateTokenViewModel.getTwitchValidateToken(accessToken)

                validateTokenViewModel.twitchValidateTokenState.collect { state ->
                    when {
                        state.isLoading -> {
                            // Keep showing loading
                        }
                        state.tokenResponse.userId.isNotEmpty() -> {
                            hideLoading()
                            redirectToStreamersActivity(accessToken)
                            return@collect
                        }
                        state.error.isNotEmpty() -> {
                            hideLoading()
                            clearTokens()
                            loginPrompt.visibility = View.VISIBLE
                            webView.visibility = View.GONE
                            return@collect
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (this::webView.isInitialized) {
            webView.destroy()
        }
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            // If WebView is visible and can go back in its history, navigate back
            webView.visibility == View.VISIBLE && webView.canGoBack() -> {
                webView.goBack()
            }
            // If WebView is visible but at the first page, return to login prompt
            webView.visibility == View.VISIBLE -> {
                webView.visibility = View.GONE
                loginPrompt.visibility = View.VISIBLE
            }
            // Otherwise, perform default back behavior
            else -> {
                super.onBackPressed()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // Hide login prompt and show WebView
        loginPrompt.visibility = View.GONE
        webView.visibility = View.VISIBLE


        // Clear WebView data to ensure fresh login (no cached Twitch session)
        webView.clearCache(true)
        webView.clearHistory()
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.CookieManager.getInstance().flush()
        android.webkit.WebStorage.getInstance().deleteAllData()
        webView.webViewClient = MyWebViewClient()
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }

        val authUrl = buildAuthUrl()
        webView.loadUrl(authUrl)
    }

    private fun buildAuthUrl(): String {
        return Constants.TWITCH_OAUTH_ENDPOINT +
                "?client_id=${Constants.TWITCH_CLIENT_ID}" +
                "&redirect_uri=${Uri.encode(Constants.TWITCH_REDIRECT_URL)}" +
                "&response_type=token" +
                "&scope=${Uri.encode(Constants.TWITCH_OAUTH_SCOPES.joinToString(" "))}"
    }

    private fun observeResponseState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.responseState.collect { state ->
                    when (state) {
                        is ResponseState.Success -> {
                            hideLoading()
                            handleSuccess(state.data?.accessToken)
                        }
                        is ResponseState.Error -> {
                            hideLoading()
                            handleError(state.message)
                        }
                        is ResponseState.Loading -> handleLoading()
                    }
                }
            }
        }
    }

    private fun handleSuccess(accessToken: String?) {
        if (accessToken.isNullOrEmpty()) {
            hideLoading()
            return
        }

        // Store the access token in encrypted storage
        securePrefs.edit()
            .putString(Constants.PREF_ACCESS_TOKEN, accessToken)
            .apply()

        // Get user info
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                handleLoading()
                userViewModel.getTwitchUser(
                    userId = null,
                    login = null,
                    authorization = "Bearer $accessToken",
                    clientId = Constants.TWITCH_CLIENT_ID
                )

                userViewModel.twitchUserState.collect { twitchUserState ->
                    when {
                        twitchUserState.isLoading -> {
                            // Already showing loading
                        }
                        twitchUserState.userList.isNotEmpty() -> {
                            val user = twitchUserState.userList[0]

                            // Store user data in encrypted storage
                            securePrefs.edit()
                                .putString(Constants.PREF_USER_ID, user.id)
                                .putString(Constants.PREF_USER_LOGIN, user.login)
                                .apply()

                            hideLoading()
                            redirectToStreamersActivity(accessToken)
                            return@collect // Stop collecting after success
                        }
                        twitchUserState.error.isNotEmpty() -> {
                            hideLoading()
                            handleError("Failed to fetch user info")
                            return@collect // Stop collecting after error
                        }
                    }
                }
            }
        }
    }

    private fun handleError(errorMessage: String?) {
        Toast.makeText(this, errorMessage ?: "An error occurred", Toast.LENGTH_SHORT).show()
    }

    private fun handleLoading() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    private fun clearTokens() {
        // Clear all encrypted data
        securePrefs.edit()
            .remove(Constants.PREF_ACCESS_TOKEN)
            .remove(Constants.PREF_USER_ID)
            .remove(Constants.PREF_USER_LOGIN)
            .apply()

        // Clear WebView data
        if (this::webView.isInitialized) {
            webView.clearCache(true)
            webView.clearHistory()
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
        }
    }

    private fun redirectToStreamersActivity(accessToken: String?) {
        val intent = Intent(this@MainActivity, TwitchStreamersActivity::class.java)
        if (accessToken != null) {
            intent.putExtra("access_token", accessToken)
        }
        startActivity(intent)
        finish()
    }

    private inner class MyWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            request?.url?.toString()?.let { url ->
                if (url.startsWith(Constants.TWITCH_REDIRECT_URL)) {
                    handleOAuthResponse(url)
                    // NOW start observing - user has completed OAuth
                    observeResponseState()
                    handleLoading() // Show loading after OAuth redirect
                    return true
                }
            }
            return super.shouldOverrideUrlLoading(view, request)
        }

        private fun handleOAuthResponse(url: String) {
            val params = parseUrlParameters(url)
            viewModel.handleOAuthResponse(params)
        }

        private fun parseUrlParameters(url: String): Map<String, String> {
            val params = HashMap<String, String>()
            val fragment = url.substringAfter("#", "")
            if (fragment.isEmpty()) return params

            fragment.split("&").forEach { part ->
                val keyValue = part.split("=")
                if (keyValue.size == 2) {
                    params[keyValue[0]] = keyValue[1]
                }
            }
            return params
        }
    }
}