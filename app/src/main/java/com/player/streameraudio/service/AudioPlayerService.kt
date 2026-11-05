package com.player.streameraudio.service

import android.content.ComponentName
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import com.player.streameraudio.domain.repository.PlaybackSettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
open class AudioPlayerService : MediaLibraryService() {

  private companion object {
    // Audio-only streams are inexpensive to buffer. A larger time-based cushion is preferable
    // to repeatedly dropping in and out of playback on unstable mobile connections.
    const val MIN_BUFFER_MS = 60_000
    const val MAX_BUFFER_MS = 180_000
    const val BUFFER_FOR_PLAYBACK_MS = 3_000
    const val BUFFER_AFTER_REBUFFER_MS = 12_000
    const val VOLUME_LEVELING_GAIN_MB = 600
    const val TAG = "AudioPlayerService"
  }

  protected var player: ExoPlayer? = null
  protected var mediaLibrarySession: MediaLibrarySession? = null
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var volumeLevelingEnabled = false
  private var activeAudioSessionId = AudioManager.ERROR
  private var loudnessEnhancer: LoudnessEnhancer? = null

  private val handler = Handler(Looper.getMainLooper())

  @Inject
  lateinit var customMediaLibrarySessionCallback: CustomMediaLibrarySessionCallback

  @Inject
  lateinit var playbackSettingsRepository: PlaybackSettingsRepository

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
      .setBufferDurationsMs(
        MIN_BUFFER_MS,
        MAX_BUFFER_MS,
        BUFFER_FOR_PLAYBACK_MS,
        BUFFER_AFTER_REBUFFER_MS
      )
      .setPrioritizeTimeOverSizeThresholds(true)
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

    player?.addListener(object : Player.Listener {
      override fun onAudioSessionIdChanged(audioSessionId: Int) {
        activeAudioSessionId = audioSessionId
        updateVolumeLeveling()
      }
    })

    serviceScope.launch {
      playbackSettingsRepository.settings
        .map { it.volumeLevelingEnabled }
        .distinctUntilChanged()
        .collect { enabled ->
          volumeLevelingEnabled = enabled
          updateVolumeLeveling()
        }
    }

    val forwardingPlayer = object : ForwardingPlayer(player!!) {
      override fun getAvailableCommands(): Player.Commands =
        super.getAvailableCommands().buildUpon()
          .remove(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
          .remove(Player.COMMAND_SEEK_BACK)
          .remove(Player.COMMAND_SEEK_FORWARD)
          .remove(Player.COMMAND_SEEK_TO_DEFAULT_POSITION)
          .remove(Player.COMMAND_SEEK_TO_MEDIA_ITEM)
          .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
          .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
          .remove(Player.COMMAND_SEEK_TO_NEXT)
          .remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
          .build()

      override fun getDuration(): Long = C.TIME_UNSET

      override fun isCurrentMediaItemSeekable(): Boolean = false
    }

    mediaLibrarySession = MediaLibrarySession.Builder(
      this,
      forwardingPlayer,
      customMediaLibrarySessionCallback
    ).build()

    val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
    val controllerFuture = MediaController.Builder(this, token).buildAsync()
    controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
    mediaLibrarySession

  private fun updateVolumeLeveling() {
    releaseVolumeLeveling()
    if (!volumeLevelingEnabled || activeAudioSessionId <= 0) return

    loudnessEnhancer = try {
      LoudnessEnhancer(activeAudioSessionId).apply {
        setTargetGain(VOLUME_LEVELING_GAIN_MB)
        setEnabled(true)
      }
    } catch (error: Exception) {
      Log.w(TAG, "Volume leveling is unavailable for this audio session", error)
      null
    }
  }

  private fun releaseVolumeLeveling() {
    try {
      loudnessEnhancer?.release()
    } catch (_: Exception) {
    } finally {
      loudnessEnhancer = null
    }
  }

  @OptIn(UnstableApi::class)
  override fun onDestroy() {
    serviceScope.cancel()
    releaseVolumeLeveling()
    try {
      mediaLibrarySession?.release()
    } catch (_: Exception) {}

    player?.release()
    player = null

    super.onDestroy()
  }
}
