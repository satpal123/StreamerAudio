package com.player.streameraudio.presentation.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.player.streameraudio.service.PlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.guava.await
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackSnapshot(
    val connected: Boolean = false,
    val mediaItem: MediaItem? = null,
    val isPlaying: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val errorMessage: String? = null,
    val isResolving: Boolean = false
)

@Singleton
class PlaybackConnection @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _snapshot = MutableStateFlow(PlaybackSnapshot())
    val snapshot: StateFlow<PlaybackSnapshot> = _snapshot
    private var browser: MediaBrowser? = null
    private var connecting = false

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = publish()
        override fun onIsPlayingChanged(isPlaying: Boolean) = publish()
        override fun onPlaybackStateChanged(playbackState: Int) = publish()
        override fun onPlayerError(error: PlaybackException) {
            _snapshot.value = _snapshot.value.copy(
                errorMessage = error.localizedMessage ?: "Playback failed",
                isResolving = false
            )
        }
    }

    fun connect() {
        if (browser != null || connecting) return
        connecting = true
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaBrowser.Builder(context, token).buildAsync()
        future.addListener({
            runCatching { future.get() }.onSuccess {
                browser = it
                connecting = false
                it.addListener(listener)
                publish()
            }.onFailure {
                connecting = false
                _snapshot.value = PlaybackSnapshot(errorMessage = it.localizedMessage)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    suspend fun children(parentId: String, page: Int = 0, pageSize: Int = 50): List<MediaItem> {
        var active = browser
        if (active == null) {
            connect()
            kotlinx.coroutines.withTimeoutOrNull(2_000L) {
                while (browser == null) {
                kotlinx.coroutines.delay(50)
                }
            }
            active = browser
        }
        val result = (active ?: error("Playback service is unavailable"))
            .getChildren(parentId, page, pageSize, null).await()
        check(result.resultCode == androidx.media3.session.SessionResult.RESULT_SUCCESS) {
            "Unable to load media library (code ${result.resultCode})"
        }
        return result.value.orEmpty()
    }

    suspend fun search(query: String, pageSize: Int = 50): List<MediaItem> {
        var active = browser
        if (active == null) {
            connect()
            kotlinx.coroutines.withTimeoutOrNull(2_000L) {
                while (browser == null) kotlinx.coroutines.delay(50)
            }
            active = browser
        }
        val connected = active ?: error("Playback service is unavailable")
        val searchResult = connected.search(query, null).await()
        check(searchResult.resultCode == androidx.media3.session.SessionResult.RESULT_SUCCESS) {
            "Unable to start search (code ${searchResult.resultCode})"
        }
        val result = connected.getSearchResult(query, 0, pageSize, null).await()
        check(result.resultCode == androidx.media3.session.SessionResult.RESULT_SUCCESS) {
            "Unable to load search results (code ${result.resultCode})"
        }
        return result.value.orEmpty()
    }

    fun play(item: MediaItem) {
        val source = item.mediaMetadata
        val playbackItem = MediaItem.Builder()
            .setMediaId(item.mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(source.title)
                    .setArtist(source.artist)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                    .setExtras(source.extras)
                    .build()
            )
            .build()
        _snapshot.value = _snapshot.value.copy(
            mediaItem = playbackItem,
            isPlaying = false,
            playbackState = Player.STATE_IDLE,
            errorMessage = null,
            isResolving = true
        )
        browser?.apply { setMediaItem(playbackItem); prepare(); play() }
    }

    fun togglePlayback() { browser?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun stop() { browser?.apply { stop(); clearMediaItems() }; publish() }

    fun retry() {
        val active = browser ?: return
        val current = active.currentMediaItem ?: return
        _snapshot.value = _snapshot.value.copy(errorMessage = null, isResolving = true)
        active.setMediaItem(MediaItem.Builder().setMediaId(current.mediaId).setMediaMetadata(current.mediaMetadata).build())
        active.prepare()
        active.play()
    }

    fun setSleepTimer(minutes: Int?) {
        val action = if (minutes == null) PlaybackService.COMMAND_CANCEL_SLEEP_TIMER else PlaybackService.COMMAND_SET_SLEEP_TIMER
        val args = android.os.Bundle().apply { if (minutes != null) putLong("duration_ms", minutes * 60_000L) }
        browser?.sendCustomCommand(SessionCommand(action, android.os.Bundle.EMPTY), args)
    }

    private fun publish() {
        val active = browser
        val currentItem = active?.currentMediaItem
        val playbackState = active?.playbackState ?: Player.STATE_IDLE
        val resolving = currentItem != null && when (playbackState) {
            Player.STATE_READY, Player.STATE_ENDED -> false
            else -> _snapshot.value.isResolving
        }
        _snapshot.value = PlaybackSnapshot(
            connected = active != null && active.isConnected,
            mediaItem = currentItem,
            isPlaying = active?.isPlaying == true,
            playbackState = playbackState,
            errorMessage = active?.playerError?.localizedMessage,
            isResolving = resolving
        )
    }
}
