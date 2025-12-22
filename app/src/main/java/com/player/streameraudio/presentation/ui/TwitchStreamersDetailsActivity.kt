package com.player.streameraudio.presentation.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bumptech.glide.Glide
import com.google.common.util.concurrent.ListenableFuture
import com.player.streameraudio.R
import com.player.streameraudio.data.util.Constants
import com.player.streameraudio.data.util.SecurePreferences
import com.player.streameraudio.presentation.viewmodel.TwitchUserViewModel
import com.player.streameraudio.service.PlaybackService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TwitchStreamersDetailsActivity : AppCompatActivity() {

    private var streamerId: String? = null
    private var streamerName: String? = null       // username / display name
    private var streamerTitle: String? = null      // stream title
    private var accessToken: String? = null

    private var streamerProfileImage: String? = null

    private val userViewModel: TwitchUserViewModel by viewModels()

    private lateinit var overlayView: View
    private lateinit var progressBar: ProgressBar
    private lateinit var toggleButton: ToggleButton
    private lateinit var uptimeTextView: TextView

    private lateinit var streamerNametextView: TextView
    private lateinit var streamDesctextView: TextView
    private lateinit var streamerProfileImageView: ImageView

    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null

    private val securePrefs: SharedPreferences by lazy {
        SecurePreferences.getEncryptedSharedPreferences(this)
    }

    private var uptimeJob: Job? = null

    // Prevent programmatic toggle updates from triggering playback
    private var ignoreToggleChange = false

    // If user tapped a streamer before controller is ready, we queue it
    private var pendingPlayRequest = false

    // âœ… NEW: Separate "user fetch" loading from "playback buffering" loading
    private var isPlaybackLoading = false
    private var isUserLoading = false

    private val playerListener = object : Player.Listener {

        @OptIn(UnstableApi::class)
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            Log.d("DetailsActivity", "onMediaItemTransition reason=$reason id=${mediaItem?.mediaId}")
            runOnUiThread { syncUiFromController() }
        }

        @OptIn(UnstableApi::class)
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            runOnUiThread {
                setToggleChecked(isPlaying)
                if (isPlaying) {
                    hideLoading()
                    startUptimeTickerIfPossible()
                } else {
                    stopUptimeTicker()
                }
            }
        }

        @OptIn(UnstableApi::class)
        override fun onPlaybackStateChanged(playbackState: Int) {
            runOnUiThread {
                when (playbackState) {
                    Player.STATE_BUFFERING -> showLoading()

                    Player.STATE_READY -> {
                        hideLoading()
                        startUptimeTickerIfPossible()
                    }

                    Player.STATE_IDLE -> {
                        // If we are trying to play, keep spinner visible
                        val tryingToPlay = controller?.playWhenReady == true
                        if (tryingToPlay) showLoading() else hideLoading()
                        stopUptimeTicker()
                    }

                    Player.STATE_ENDED -> {
                        hideLoading()
                        stopUptimeTicker()
                    }
                }
            }
        }

        @OptIn(UnstableApi::class)
        override fun onPlayerError(error: PlaybackException) {
            Log.e("DetailsActivity", "Playback error: ${error.message}", error)
            runOnUiThread {
                hideLoading()
                stopUptimeTicker()
                Toast.makeText(
                    this@TwitchStreamersDetailsActivity,
                    "Playback error: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
                setToggleChecked(false)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.P)
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_twitch_streamers_details)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Views
        streamerNametextView = findViewById(R.id.streamerNametextView)
        streamDesctextView = findViewById(R.id.streamDesctextView)
        streamerProfileImageView = findViewById(R.id.streamerimageView)
        progressBar = findViewById(R.id.progressBar)
        overlayView = findViewById(R.id.overlayView)
        toggleButton = findViewById(R.id.toggleButton)
        uptimeTextView = findViewById(R.id.uptimeTextView)

        toggleButton.isEnabled = false
        uptimeTextView.text = "Live"

        // Read initial intent
        readIntent(intent)

        // Toggle button
        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (ignoreToggleChange) return@setOnCheckedChangeListener

            val c = controller
            if (c == null) {
                Toast.makeText(this, "Player not ready", Toast.LENGTH_SHORT).show()
                setToggleChecked(false)
                return@setOnCheckedChangeListener
            }

            if (!isChecked) {
                c.pause()
                return@setOnCheckedChangeListener
            }

            // If selected streamer differs from current -> switch, else resume
            val selectedId = streamerId
            val currentId = c.currentMediaItem?.mediaId
            if (!selectedId.isNullOrBlank() && selectedId != currentId) {
                requestPlaySelected()
            } else {
                c.play()
            }
        }

        // Build controller and attach listener reliably
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({
            val c = controllerFuture.get()
            controller = c
            c.addListener(playerListener)

            runOnUiThread {
                toggleButton.isEnabled = true

                // Always reflect current session first (Android Auto / mini player may already be playing)
                syncUiFromController()

                // âœ… FIX: If we opened from mini-player and playback already ready/playing, donâ€™t leave spinner up
                if (c.playbackState == Player.STATE_READY || c.isPlaying) {
                    hideLoading()
                }

                // If we were opened with a streamerId and it differs from what's loaded, switch+play if requested
                val selectedId = streamerId
                val currentId = c.currentMediaItem?.mediaId
                val forcePlay = intent.getBooleanExtra("forcePlay", false)

                if (!selectedId.isNullOrBlank() && selectedId != currentId) {
                    pendingPlayRequest = pendingPlayRequest || forcePlay
                    if (pendingPlayRequest || forcePlay) {
                        pendingPlayRequest = false
                        requestPlaySelected()
                    }
                } else {
                    // Same stream already loaded -> just show correct play state
                    setToggleChecked(c.isPlaying)
                }
            }
        }, mainExecutor)

        // Observe user info (artwork etc.) - do NOT block playback
        lifecycleScope.launch {
            userViewModel.twitchUserState.collect { state ->
                when {
                    state.isLoading -> showLoadingUserData()
                    state.userList.isNotEmpty() -> {
                        hideLoadingUserData()
                        streamerProfileImage = state.userList[0].profileImageUrl

                        // Update image if it matches selected stream OR nothing playing yet
                        val c = controller
                        val currentId = c?.currentMediaItem?.mediaId
                        if (currentId == null || currentId == streamerId) {
                            streamerProfileImage?.let {
                                Glide.with(this@TwitchStreamersDetailsActivity)
                                    .load(it)
                                    .into(streamerProfileImageView)
                            }
                        }
                    }
                    state.error.isNotEmpty() -> {
                        hideLoadingUserData()
                        Toast.makeText(this@TwitchStreamersDetailsActivity, state.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Trigger user load (for artwork)
        fetchUserIfPossible()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @OptIn(UnstableApi::class)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent)

        pendingPlayRequest = true
        fetchUserIfPossible()

        if (controller != null) {
            pendingPlayRequest = false
            requestPlaySelected()
        }
    }

    private fun readIntent(intent: Intent) {
        streamerId = intent.getStringExtra("streamerId")
        streamerName = intent.getStringExtra("streamerName")
        streamerTitle = intent.getStringExtra("streamerTitle")

        accessToken = intent.getStringExtra("access_token")
            ?: securePrefs.getString(Constants.PREF_ACCESS_TOKEN, null)

        val forcePlay = intent.getBooleanExtra("forcePlay", false)
        if (forcePlay) pendingPlayRequest = true

        // UI immediately (phone navigation)
        if (!streamerName.isNullOrBlank()) streamerNametextView.text = streamerName
        if (!streamerTitle.isNullOrBlank()) streamDesctextView.text = streamerTitle
    }

    private fun fetchUserIfPossible() {
        val id = streamerId ?: return
        val token = accessToken ?: return

        userViewModel.getTwitchUser(
            userId = id,
            login = null,
            authorization = "Bearer $token",
            clientId = Constants.TWITCH_CLIENT_ID
        )
    }

    @OptIn(UnstableApi::class)
    private fun requestPlaySelected() {
        runOnUiThread { showLoading() }
        val c = controller ?: return
        val id = streamerId

        if (id.isNullOrBlank()) {
            Toast.makeText(this, "Stream info not available", Toast.LENGTH_SHORT).show()
            setToggleChecked(false)
            hideLoading()
            return
        }

        stopUptimeTicker()
        uptimeTextView.text = "Live"

        // Correct mapping:
        // title = stream title, artist = streamer name
        val metaBuilder = MediaMetadata.Builder()
            .setTitle(streamerTitle ?: "")
            .setArtist(streamerName ?: "")
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)

        streamerProfileImage?.let {
            if (it.isNotBlank()) metaBuilder.setArtworkUri(Uri.parse(it))
        }

        val mediaItem = MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(metaBuilder.build())
            .build()

        c.setMediaItem(mediaItem)
        c.prepare()
        c.play()

        setToggleChecked(true)
    }

    @OptIn(UnstableApi::class)
    private fun syncUiFromController() {
        val c = controller ?: return
        val item = c.currentMediaItem ?: run {
            setToggleChecked(false)
            uptimeTextView.text = "Live"
            return
        }

        val meta = item.mediaMetadata

        // Service may put "Name - uptime" into artist
        val rawArtist = meta.artist?.toString().orEmpty()
        val cleanName = if (rawArtist.contains(" - ")) rawArtist.substringBefore(" - ") else rawArtist

        streamerNametextView.text = cleanName.ifBlank { streamerName ?: "Now Playing" }
        streamDesctextView.text = meta.title?.toString().orEmpty()

        meta.artworkUri?.let { uri ->
            Glide.with(this).load(uri).into(streamerProfileImageView)
        }

        setToggleChecked(c.isPlaying)
        startUptimeTickerIfPossible()
    }

    private fun setToggleChecked(value: Boolean) {
        ignoreToggleChange = true
        toggleButton.isChecked = value
        ignoreToggleChange = false
    }

    // -------------------------
    // âœ… LOADING (fixed)
    // -------------------------

    // User fetch: show spinner but no overlay (donâ€™t block UI)
    private fun showLoadingUserData() {
        isUserLoading = true
        progressBar.visibility = View.VISIBLE
        overlayView.visibility = View.GONE
    }

    // Hide user-loading; keep playback loading if active
    private fun hideLoadingUserData() {
        isUserLoading = false
        toggleButton.isEnabled = true

        if (!isPlaybackLoading) {
            progressBar.visibility = View.GONE
            overlayView.visibility = View.GONE
        }
    }

    // Playback loading: show spinner + overlay
    private fun showLoading() {
        isPlaybackLoading = true
        progressBar.visibility = View.VISIBLE
        overlayView.visibility = View.VISIBLE
    }

    // Stop playback loading; keep spinner if user load still running
    private fun hideLoading() {
        isPlaybackLoading = false

        if (isUserLoading) {
            progressBar.visibility = View.VISIBLE
            overlayView.visibility = View.GONE
        } else {
            progressBar.visibility = View.GONE
            overlayView.visibility = View.GONE
        }
    }

    // -------------------------
    // Uptime ticker (phone UI)
    // -------------------------

    @SuppressLint("SetTextI18n")
    private fun startUptimeTickerIfPossible() {
        val c = controller ?: return
        val startedAtMs = c.currentMediaItem
            ?.mediaMetadata
            ?.extras
            ?.getLong("startedAtMs", -1L)
            ?.takeIf { it > 0 } ?: return

        uptimeJob?.cancel()
        uptimeJob = lifecycleScope.launch {
            while (isActive) {
                uptimeTextView.text = "Live - ${formatUptime(startedAtMs)}"
                delay(1000)
            }
        }
    }

    private fun stopUptimeTicker() {
        uptimeJob?.cancel()
        uptimeJob = null
    }

    private fun formatUptime(startedAtMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val diff = (nowMs - startedAtMs).coerceAtLeast(0L)
        val totalSeconds = diff / 1000L
        val hours = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60

        return if (hours > 0) String.format("%dh %02dm", hours, mins)
        else String.format("%dm %02ds", mins, secs)
    }

    @OptIn(UnstableApi::class)
    override fun onDestroy() {
        stopUptimeTicker()
        controller?.removeListener(playerListener)
        controller = null

        if (::controllerFuture.isInitialized) {
            MediaController.releaseFuture(controllerFuture)
        }
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }
}