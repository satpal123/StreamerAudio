package com.player.streameraudio.presentation.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.player.streameraudio.R
import com.player.streameraudio.data.util.Constants
import com.player.streameraudio.data.util.SecurePreferences
import com.player.streameraudio.service.PlaybackService

@UnstableApi
class SettingsActivity : AppCompatActivity() {

    private lateinit var streamSourceRadioGroup: RadioGroup
    private lateinit var proxyRadioGroup: RadioGroup
    private lateinit var hlsRadio: RadioButton
    private lateinit var proxyRadio: RadioButton
    private lateinit var logoutButton: Button

    private var controllerFuture: ListenableFuture<MediaController>? = null

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Setup action bar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Settings"
        }

        streamSourceRadioGroup = findViewById(R.id.streamSourceRadioGroup)
        proxyRadioGroup = findViewById(R.id.proxyRadioGroup)
        hlsRadio = findViewById(R.id.hlsRadio)
        proxyRadio = findViewById(R.id.proxyRadio)
        logoutButton = findViewById(R.id.logoutButton)

        // Load current settings
        loadSettings()

        // Check if user is logged in
        val securePrefs = SecurePreferences.getEncryptedSharedPreferences(this)
        val accessToken = securePrefs.getString(Constants.PREF_ACCESS_TOKEN, null)

        // Show/hide logout button based on login status
        if (accessToken.isNullOrBlank()) {
            logoutButton.visibility = android.view.View.GONE
        } else {
            logoutButton.visibility = android.view.View.VISIBLE
            logoutButton.setOnClickListener {
                showLogoutConfirmation()
            }
        }

        // Handle stream source changes
        streamSourceRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.hlsRadio -> {
                    Constants.setStreamSource(this, Constants.STREAM_SOURCE_HLS)
                    proxyRadioGroup.isEnabled = false
                    disableProxyOptions()
                    Toast.makeText(this, "Using Twitch HLS (Direct)", Toast.LENGTH_SHORT).show()
                }
                R.id.proxyRadio -> {
                    Constants.setStreamSource(this, Constants.STREAM_SOURCE_PROXY)
                    proxyRadioGroup.isEnabled = true
                    enableProxyOptions()
                    Toast.makeText(this, "Using Proxy Servers", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Handle proxy selection changes
        proxyRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedProxy = when (checkedId) {
                R.id.proxyEuRadio -> Constants.PROXY_EU
                R.id.proxyEu2Radio -> Constants.PROXY_EU2
                R.id.proxyEu3Radio -> Constants.PROXY_EU3
                R.id.proxyAsRadio -> Constants.PROXY_AS
                else -> Constants.PROXY_AS
            }

            val prefs = SecurePreferences.getEncryptedSharedPreferences(this)
            prefs.edit().putString(Constants.PREF_SELECTED_PROXY, selectedProxy).apply()

            Toast.makeText(this, "Proxy changed", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout? You can still browse categories without logging in.")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun performLogout() {
        // Stop any playing media first
        try {
            val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
            controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

            controllerFuture?.addListener({
                try {
                    val controller = controllerFuture?.get()
                    controller?.stop()
                    controller?.clearMediaItems()
                    MediaController.releaseFuture(controllerFuture!!)
                } catch (e: Exception) {
                    // Ignore errors if controller not available
                }
            }, mainExecutor)
        } catch (e: Exception) {
            // Ignore errors if service not running
        }

        // Clear WebView cookies and cache (so Twitch will ask for login again)
        try {
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
            android.webkit.WebStorage.getInstance().deleteAllData()
        } catch (e: Exception) {
            // Ignore if WebView not available
        }

        // Clear all user data from encrypted preferences
        val prefs = SecurePreferences.getEncryptedSharedPreferences(this)
        prefs.edit()
            .remove(Constants.PREF_ACCESS_TOKEN)
            .remove(Constants.PREF_USER_ID)
            .remove(Constants.PREF_USER_LOGIN)
            .apply()

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Redirect to MainActivity (which will show the login prompt)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadSettings() {
        // Load stream source
        val streamSource = Constants.getStreamSource(this)
        when (streamSource) {
            Constants.STREAM_SOURCE_HLS -> {
                hlsRadio.isChecked = true
                proxyRadioGroup.isEnabled = false
                disableProxyOptions()
            }
            Constants.STREAM_SOURCE_PROXY -> {
                proxyRadio.isChecked = true
                proxyRadioGroup.isEnabled = true
                enableProxyOptions()
            }
        }

        // Load proxy selection
        val selectedProxy = Constants.getProxyUrl(this)
        when (selectedProxy) {
            Constants.PROXY_EU -> findViewById<RadioButton>(R.id.proxyEuRadio).isChecked = true
            Constants.PROXY_EU2 -> findViewById<RadioButton>(R.id.proxyEu2Radio).isChecked = true
            Constants.PROXY_EU3 -> findViewById<RadioButton>(R.id.proxyEu3Radio).isChecked = true
            Constants.PROXY_AS -> findViewById<RadioButton>(R.id.proxyAsRadio).isChecked = true
        }
    }

    private fun disableProxyOptions() {
        for (i in 0 until proxyRadioGroup.childCount) {
            proxyRadioGroup.getChildAt(i).isEnabled = false
        }
    }

    private fun enableProxyOptions() {
        for (i in 0 until proxyRadioGroup.childCount) {
            proxyRadioGroup.getChildAt(i).isEnabled = true
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            controllerFuture?.let { MediaController.releaseFuture(it) }
        } catch (e: Exception) {
            // Ignore
        }
    }
}