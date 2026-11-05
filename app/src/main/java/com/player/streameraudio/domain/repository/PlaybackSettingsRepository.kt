package com.player.streameraudio.domain.repository

import com.player.streameraudio.data.settings.PlaybackMode
import com.player.streameraudio.data.settings.LastPlayedStream
import com.player.streameraudio.data.settings.ProxyEndpoint
import com.player.streameraudio.data.settings.SavedStream
import com.player.streameraudio.data.settings.UserSettings
import kotlinx.coroutines.flow.StateFlow

interface PlaybackSettingsRepository {
    val settings: StateFlow<UserSettings>
    suspend fun addProxy(name: String, baseUrl: String): Result<ProxyEndpoint>
    suspend fun updateProxy(id: String, name: String, baseUrl: String): Result<ProxyEndpoint>
    suspend fun deleteProxy(id: String)
    suspend fun selectProxy(id: String?)
    suspend fun setMode(mode: PlaybackMode): Result<Unit>
    suspend fun setFallbackEnabled(enabled: Boolean)
    suspend fun consumeLegacyProxyNotice()
    suspend fun markLegacyProxyReset()
    suspend fun setSleepTimerDeadline(deadlineEpochMs: Long?)
    suspend fun setLastPlayedStream(stream: LastPlayedStream?)
    suspend fun getLastPlayedStream(): LastPlayedStream?
    suspend fun addRecentStream(stream: SavedStream)
    suspend fun toggleFavorite(stream: SavedStream): Boolean
    suspend fun clearRecentStreams()
    suspend fun setVolumeLevelingEnabled(enabled: Boolean)
}
