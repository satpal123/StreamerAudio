package com.player.streameraudio.service

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.player.streameraudio.presentation.ui.MainActivity
import com.player.streameraudio.presentation.ui.TwitchStreamersActivity
import com.player.streameraudio.presentation.ui.TwitchStreamersDetailsActivity

class PlaybackService : AudioPlayerService() {

  companion object {
    private const val IMMUTABLE_FLAG = PendingIntent.FLAG_IMMUTABLE
    private const val UPDATE_FLAG = PendingIntent.FLAG_UPDATE_CURRENT
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
    val intent = Intent(this, TwitchStreamersDetailsActivity::class.java).apply {
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

  // Optional helpers (not overrides anymore)

  private fun getSingleTopActivity(): PendingIntent? {
    val intent = Intent(this, TwitchStreamersDetailsActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }

    return PendingIntent.getActivity(
      this,
      0,
      intent,
      IMMUTABLE_FLAG or UPDATE_FLAG
    )
  }

  private fun getBackStackedActivity(): PendingIntent? {
    return TaskStackBuilder.create(this).run {
      addNextIntent(Intent(this@PlaybackService, MainActivity::class.java))
      addNextIntent(Intent(this@PlaybackService, TwitchStreamersActivity::class.java))

      val playerIntent =
        Intent(this@PlaybackService, TwitchStreamersDetailsActivity::class.java).apply {
          player?.currentMediaItem?.let { mediaItem ->
            val streamerId = mediaItem.mediaId
            val rawArtist = mediaItem.mediaMetadata.artist?.toString().orEmpty()
            val streamerName =
              rawArtist.substringBefore(" - ").trim().ifBlank { rawArtist }
            val streamerTitle = mediaItem.mediaMetadata.title?.toString()

            putExtra("streamerId", streamerId)
            putExtra("streamerName", streamerName)
            putExtra("streamerTitle", streamerTitle)
          }

          flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

      addNextIntent(playerIntent)
      getPendingIntent(0, IMMUTABLE_FLAG or UPDATE_FLAG)
    }
  }
}
