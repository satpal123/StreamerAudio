package com.player.streameraudio.service

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.player.streameraudio.presentation.ui.MainActivity

class PlaybackService : AudioPlayerService() {

  companion object {
    private const val IMMUTABLE_FLAG = PendingIntent.FLAG_IMMUTABLE
    private const val UPDATE_FLAG = PendingIntent.FLAG_UPDATE_CURRENT
    const val COMMAND_RETRY_STREAM = "com.player.streameraudio.action.RETRY_STREAM"
    const val COMMAND_SET_SLEEP_TIMER = "com.player.streameraudio.action.SET_SLEEP_TIMER"
    const val COMMAND_CANCEL_SLEEP_TIMER = "com.player.streameraudio.action.CANCEL_SLEEP_TIMER"
  }

  private var sessionActivityListenerAdded = false

  @OptIn(UnstableApi::class)
  override fun onCreate() {
    super.onCreate()

    // Set initial session activity (important when AA launches the service)
    updateSessionActivity()

    if (!sessionActivityListenerAdded) {
      sessionActivityListenerAdded = true

      player?.addListener(object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
          updateSessionActivity()
        }

        override fun onMediaMetadataChanged(
          mediaMetadata: androidx.media3.common.MediaMetadata
        ) {
          updateSessionActivity()
        }
      })
    }
  }

  @OptIn(UnstableApi::class)
  private fun updateSessionActivity() {
    val intent = Intent(this, MainActivity::class.java).apply {
      player?.currentMediaItem?.let { mediaItem ->
        val streamerId = mediaItem.mediaId
        val rawArtist = mediaItem.mediaMetadata.artist?.toString().orEmpty()
        val streamerName = rawArtist.substringBefore(" - ").trim().ifBlank { rawArtist }
        val streamerTitle = mediaItem.mediaMetadata.title?.toString()

        Log.d(
          "PlaybackService",
          "Updating Session Activity - ID: $streamerId, Name: $streamerName, Title: $streamerTitle"
        )

        putExtra("streamerId", streamerId)
        putExtra("streamerName", streamerName)
        putExtra("streamerTitle", streamerTitle)
      }

      flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }

    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      intent,
      IMMUTABLE_FLAG or UPDATE_FLAG
    )

    mediaLibrarySession?.setSessionActivity(pendingIntent)
  }

}
