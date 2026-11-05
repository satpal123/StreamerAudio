package com.player.streameraudio.data.repository

import androidx.datastore.core.DataStore
import com.player.streameraudio.data.settings.PlaybackMode
import com.player.streameraudio.data.settings.PlaybackRouting
import com.player.streameraudio.data.settings.LastPlayedStream
import com.player.streameraudio.data.settings.ProxyEndpoint
import com.player.streameraudio.data.settings.ProxyEndpointValidator
import com.player.streameraudio.data.settings.ProxyValidationResult
import com.player.streameraudio.data.settings.SavedStream
import com.player.streameraudio.data.settings.UserSettings
import com.player.streameraudio.domain.repository.PlaybackSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<UserSettings>
) : PlaybackSettingsRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override val settings: StateFlow<UserSettings> = dataStore.data.stateIn(
        scope,
        SharingStarted.Eagerly,
        UserSettings()
    )

    override suspend fun addProxy(name: String, baseUrl: String): Result<ProxyEndpoint> {
        val normalized = normalized(name, baseUrl).getOrElse { return Result.failure(it) }
        val endpoint = ProxyEndpoint(UUID.randomUUID().toString(), name.trim(), normalized)
        return runCatching {
            dataStore.updateData { current ->
                check(current.proxies.none { it.baseUrl.equals(normalized, ignoreCase = true) }) {
                    "That proxy URL is already saved"
                }
                current.copy(proxies = current.proxies + endpoint)
            }
            endpoint
        }
    }

    override suspend fun updateProxy(id: String, name: String, baseUrl: String): Result<ProxyEndpoint> {
        val normalized = normalized(name, baseUrl).getOrElse { return Result.failure(it) }
        val endpoint = ProxyEndpoint(id, name.trim(), normalized)
        return runCatching {
            dataStore.updateData { current ->
                check(current.proxies.any { it.id == id }) { "Proxy not found" }
                check(current.proxies.none { it.id != id && it.baseUrl.equals(normalized, true) }) {
                    "That proxy URL is already saved"
                }
                current.copy(proxies = current.proxies.map { if (it.id == id) endpoint else it })
            }
            endpoint
        }
    }

    override suspend fun deleteProxy(id: String) {
        dataStore.updateData { current ->
            val selectedWasDeleted = current.routing.selectedProxyId == id
            current.copy(
                proxies = current.proxies.filterNot { it.id == id },
                routing = if (selectedWasDeleted) PlaybackRouting() else current.routing
            )
        }
    }

    override suspend fun selectProxy(id: String?) {
        dataStore.updateData { current ->
            require(id == null || current.proxies.any { it.id == id }) { "Proxy not found" }
            current.copy(routing = current.routing.copy(selectedProxyId = id))
        }
    }

    override suspend fun setMode(mode: PlaybackMode): Result<Unit> = runCatching<Unit> {
        dataStore.updateData { current ->
            if (mode == PlaybackMode.PROXY) {
                require(current.routing.selectedProxyId != null) { "Add and select a proxy first" }
            }
            current.copy(routing = current.routing.copy(mode = mode))
        }
    }

    override suspend fun setFallbackEnabled(enabled: Boolean) {
        dataStore.updateData { current ->
            val canEnable = current.routing.selectedProxyId != null
            current.copy(routing = current.routing.copy(fallbackEnabled = enabled && canEnable))
        }
    }

    override suspend fun consumeLegacyProxyNotice() {
        dataStore.updateData { it.copy(legacyProxyNoticePending = false) }
    }

    override suspend fun markLegacyProxyReset() {
        dataStore.updateData { it.copy(routing = PlaybackRouting(), legacyProxyNoticePending = true) }
    }

    override suspend fun setSleepTimerDeadline(deadlineEpochMs: Long?) {
        dataStore.updateData { it.copy(sleepTimerDeadlineEpochMs = deadlineEpochMs) }
    }

    override suspend fun setLastPlayedStream(stream: LastPlayedStream?) {
        dataStore.updateData { it.copy(lastPlayedStream = stream) }
    }

    override suspend fun getLastPlayedStream(): LastPlayedStream? =
        dataStore.data.first().lastPlayedStream

    override suspend fun addRecentStream(stream: SavedStream) {
        dataStore.updateData { current ->
            current.copy(
                recentStreams = (listOf(stream) + current.recentStreams.filterNot {
                    it.stableKey == stream.stableKey
                }).take(MAX_RECENT_STREAMS)
            )
        }
    }

    override suspend fun toggleFavorite(stream: SavedStream): Boolean {
        var isFavorite = false
        dataStore.updateData { current ->
            val exists = current.favoriteStreams.any { it.stableKey == stream.stableKey }
            isFavorite = !exists
            current.copy(
                favoriteStreams = if (exists) {
                    current.favoriteStreams.filterNot { it.stableKey == stream.stableKey }
                } else {
                    (listOf(stream) + current.favoriteStreams).take(MAX_FAVORITE_STREAMS)
                }
            )
        }
        return isFavorite
    }

    override suspend fun clearRecentStreams() {
        dataStore.updateData { it.copy(recentStreams = emptyList()) }
    }

    override suspend fun setVolumeLevelingEnabled(enabled: Boolean) {
        dataStore.updateData { it.copy(volumeLevelingEnabled = enabled) }
    }

    private fun normalized(name: String, baseUrl: String): Result<String> =
        when (val validation = ProxyEndpointValidator.validate(name, baseUrl)) {
            is ProxyValidationResult.Valid -> Result.success(validation.normalizedUrl)
            is ProxyValidationResult.Invalid -> Result.failure(IllegalArgumentException(validation.message))
        }

    private companion object {
        const val MAX_RECENT_STREAMS = 50
        const val MAX_FAVORITE_STREAMS = 100
    }
}
