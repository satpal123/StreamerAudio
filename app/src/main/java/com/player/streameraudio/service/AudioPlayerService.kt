package com.player.streameraudio.service

import android.content.ComponentName
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.player.streameraudio.data.util.Constants
import com.player.streameraudio.data.util.SecurePreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
open class AudioPlayerService : MediaLibraryService() {

  protected var player: ExoPlayer? = null
  protected var mediaLibrarySession: MediaLibrarySession? = null
  private lateinit var audioManager: AudioManager

  private val handler = Handler(Looper.getMainLooper())

  @Inject
  lateinit var customMediaLibrarySessionCallback: CustomMediaLibrarySessionCallback

  private val securePrefs: SharedPreferences by lazy {
    SecurePreferences.getEncryptedSharedPreferences(this)
  }

  @OptIn(UnstableApi::class)
  override fun onCreate() {
    super.onCreate()

    val httpFactory = DefaultHttpDataSource.Factory()
      .setUserAgent(Constants.USER_AGENT)
      .setAllowCrossProtocolRedirects(true)

    val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)
    val mediaSourceFactory =
      DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory)

    val trackSelector = DefaultTrackSelector(this).apply {
      parameters = buildUponParameters()
        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
        .build()
    }

    val loadControl = DefaultLoadControl.Builder()
      .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
      .setBufferDurationsMs(30_000, 120_000, 2_000, 5_000)
      .build()

    player = ExoPlayer.Builder(this)
      .setMediaSourceFactory(mediaSourceFactory)
      .setTrackSelector(trackSelector)
      .setLoadControl(loadControl)
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
          .setUsage(C.USAGE_MEDIA)
          .build(),
        true
      )
      .setHandleAudioBecomingNoisy(true)
      .setWakeMode(C.WAKE_MODE_NETWORK)
      .build()

    val forwardingPlayer = object : ForwardingPlayer(player!!) {
      override fun getAvailableCommands(): Player.Commands =
        super.getAvailableCommands().buildUpon()
          .remove(Player.COMMAND_SEEK_BACK)
          .remove(Player.COMMAND_SEEK_FORWARD)
          .build()

      override fun getDuration(): Long = C.TIME_UNSET
    }

    mediaLibrarySession = MediaLibrarySession.Builder(
      this,
      forwardingPlayer,
      customMediaLibrarySessionCallback
    ).build()

    audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

    val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
    val controllerFuture = MediaController.Builder(this, token).buildAsync()
    controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
    mediaLibrarySession

  @OptIn(UnstableApi::class)
  override fun onDestroy() {
    try {
      mediaLibrarySession?.release()
    } catch (_: Exception) {}

    player?.release()
    player = null

    super.onDestroy()
  }
}