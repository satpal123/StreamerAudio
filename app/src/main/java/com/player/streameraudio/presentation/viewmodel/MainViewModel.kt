package com.player.streameraudio.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.player.streameraudio.BuildConfig
import com.player.streameraudio.data.auth.AuthSession
import com.player.streameraudio.data.auth.LegacyAuthMigrator
import com.player.streameraudio.data.auth.TokenStore
import com.player.streameraudio.data.settings.PlaybackMode
import com.player.streameraudio.data.settings.ProxyEndpoint
import com.player.streameraudio.data.settings.UserSettings
import com.player.streameraudio.data.util.Constants
import com.player.streameraudio.data.util.ResponseState
import com.player.streameraudio.domain.repository.PlaybackSettingsRepository
import com.player.streameraudio.domain.repository.TwitchRepository
import com.player.streameraudio.domain.usecase.GetValidTokenUseCase
import com.player.streameraudio.presentation.playback.PlaybackConnection
import com.player.streameraudio.presentation.playback.PlaybackSnapshot
import com.player.streameraudio.service.CustomMediaLibrarySessionCallback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject

data class MainUiState(
    val authenticated: Boolean = false,
    val userLogin: String? = null,
    val loading: Boolean = false,
    val title: String = "Browse",
    val items: List<MediaItem> = emptyList(),
    val errorMessage: String? = null,
    val playback: PlaybackSnapshot = PlaybackSnapshot(),
    val settings: UserSettings = UserSettings(),
    val oauthState: String? = null,
    val currentParentId: String? = null,
    val currentPage: Int = 0,
    val canLoadMore: Boolean = false,
    val loadingMore: Boolean = false,
    val searchQuery: String? = null,
    val profileImageUrl: String? = null,
    val selectedSleepTimerMinutes: Int? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val playbackConnection: PlaybackConnection,
    private val settingsRepository: PlaybackSettingsRepository,
    private val tokenStore: TokenStore,
    private val legacyAuthMigrator: LegacyAuthMigrator,
    private val getValidTokenUseCase: GetValidTokenUseCase,
    private val twitchRepository: TwitchRepository,
    private val okHttpClient: OkHttpClient
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState(oauthState = savedStateHandle[OAUTH_STATE_KEY]))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (legacyAuthMigrator.migrateOnce()) settingsRepository.markLegacyProxyReset()
            val session = tokenStore.read()
            _uiState.update { it.copy(authenticated = session != null, userLogin = session?.userLogin) }
            validateStoredSession(session)
        }
        playbackConnection.connect()
        viewModelScope.launch { playbackConnection.snapshot.collectLatest { value -> _uiState.update { it.copy(playback = value) } } }
        viewModelScope.launch {
            settingsRepository.settings.collectLatest { value ->
                _uiState.update {
                    it.copy(
                        settings = value,
                        selectedSleepTimerMinutes = if (value.sleepTimerDeadlineEpochMs == null) null else it.selectedSleepTimerMinutes
                    )
                }
            }
        }
    }

    fun loadSection(parentId: String, title: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    loadingMore = false,
                    title = title,
                    errorMessage = null,
                    currentParentId = parentId,
                    currentPage = 0,
                    canLoadMore = false,
                    searchQuery = null
                )
            }
            if (BuildConfig.USE_DEMO_CONTENT) {
                _uiState.update { it.copy(loading = false, items = demoItems(parentId)) }
                return@launch
            }
            runCatching { playbackConnection.children(parentId) }
                .onSuccess { items ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            items = items,
                            canLoadMore = items.size >= PAGE_SIZE
                        )
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(loading = false, errorMessage = error.localizedMessage ?: "Unable to load streams") } }
        }
    }

    fun loadNextPage() {
        val current = _uiState.value
        val parentId = current.currentParentId ?: return
        if (current.loading || current.loadingMore || !current.canLoadMore) return
        val nextPage = current.currentPage + 1
        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true, errorMessage = null) }
            runCatching { playbackConnection.children(parentId, nextPage, PAGE_SIZE) }
                .onSuccess { newItems ->
                    _uiState.update { state ->
                        state.copy(
                            items = (state.items + newItems).distinctBy { it.mediaId },
                            currentPage = nextPage,
                            canLoadMore = newItems.size >= PAGE_SIZE,
                            loadingMore = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            loadingMore = false,
                            errorMessage = error.localizedMessage ?: "Unable to load more"
                        )
                    }
                }
        }
    }

    fun searchChannels(query: String) {
        val normalized = query.trim()
        if (normalized.length < 2) {
            _uiState.update { it.copy(errorMessage = "Enter at least two characters") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    loadingMore = false,
                    title = "Search",
                    items = emptyList(),
                    errorMessage = null,
                    currentParentId = null,
                    canLoadMore = false,
                    searchQuery = normalized
                )
            }
            runCatching { playbackConnection.search(normalized) }
                .onSuccess { items -> _uiState.update { it.copy(loading = false, items = items) } }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(loading = false, errorMessage = error.localizedMessage ?: "Search failed")
                    }
                }
        }
    }

    fun reloadCurrentSection() {
        val current = _uiState.value
        current.currentParentId?.let { loadSection(it, current.title) }
    }

    fun play(item: MediaItem) = playbackConnection.play(item)
    fun togglePlayback() = playbackConnection.togglePlayback()
    fun retryPlayback() = playbackConnection.retry()
    fun setSleepTimer(minutes: Int?) {
        _uiState.update { it.copy(selectedSleepTimerMinutes = minutes) }
        playbackConnection.setSleepTimer(minutes)
    }

    fun createAuthUrl(): Uri {
        val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
        val state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        savedStateHandle[OAUTH_STATE_KEY] = state
        _uiState.update { it.copy(oauthState = state, errorMessage = null) }
        return Uri.parse(Constants.TWITCH_OAUTH_ENDPOINT).buildUpon()
            .appendQueryParameter("client_id", Constants.TWITCH_CLIENT_ID)
            .appendQueryParameter("redirect_uri", Constants.TWITCH_REDIRECT_URL)
            .appendQueryParameter("response_type", "token")
            .appendQueryParameter("scope", Constants.TWITCH_OAUTH_SCOPES.joinToString(" "))
            .appendQueryParameter("state", state)
            .build()
    }

    fun handleAuthResult(uri: Uri?) {
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Twitch sign-in was cancelled") }
            return
        }
        val fragmentValues = uri.fragment.orEmpty().split('&').mapNotNull {
            val pair = it.split('=', limit = 2)
            if (pair.size == 2) pair[0] to Uri.decode(pair[1]) else null
        }.toMap()
        val queryValues = uri.queryParameterNames.associateWith { uri.getQueryParameter(it).orEmpty() }
        val values = fragmentValues + queryValues
        if (values["state"] != _uiState.value.oauthState) {
            clearOAuthState()
            _uiState.update { it.copy(errorMessage = "Twitch sign-in could not be verified") }
            return
        }
        val token = values["access_token"]
        if (token.isNullOrBlank()) {
            clearOAuthState()
            _uiState.update { it.copy(errorMessage = values["error_description"] ?: "Twitch sign-in failed") }
            return
        }
        viewModelScope.launch { validateAndSave(token) }
    }

    fun logout() {
        playbackConnection.stop()
        _uiState.update { it.copy(authenticated = false, userLogin = null, profileImageUrl = null, items = emptyList()) }
        viewModelScope.launch {
            val session = tokenStore.read()
            session?.let { revokeToken(it.accessToken) }
            tokenStore.clear()
        }
    }

    fun addProxy(name: String, url: String, onResult: (Result<ProxyEndpoint>) -> Unit) {
        viewModelScope.launch { onResult(settingsRepository.addProxy(name, url)) }
    }

    fun updateProxy(id: String, name: String, url: String, onResult: (Result<ProxyEndpoint>) -> Unit) {
        viewModelScope.launch { onResult(settingsRepository.updateProxy(id, name, url)) }
    }

    fun deleteProxy(id: String) {
        val selected = _uiState.value.settings.routing.selectedProxyId == id
        viewModelScope.launch {
            settingsRepository.deleteProxy(id)
            if (selected) {
                _uiState.update { it.copy(errorMessage = "Selected proxy removed. Playback returned to Direct.") }
            }
        }
    }
    fun selectProxy(id: String?) { viewModelScope.launch { settingsRepository.selectProxy(id) } }
    fun setMode(mode: PlaybackMode, onResult: (Result<Unit>) -> Unit = {}) { viewModelScope.launch { onResult(settingsRepository.setMode(mode)) } }
    fun setFallback(enabled: Boolean) { viewModelScope.launch { settingsRepository.setFallbackEnabled(enabled) } }
    fun consumeLegacyNotice() { viewModelScope.launch { settingsRepository.consumeLegacyProxyNotice() } }
    fun clearMessage() { _uiState.update { it.copy(errorMessage = null) } }

    private suspend fun validateStoredSession(session: AuthSession?) {
        if (session == null) return
        getValidTokenUseCase(session.accessToken).collect { response ->
            when (response) {
                is ResponseState.Success -> {
                    val validated = response.data ?: return@collect
                    tokenStore.write(session.copy(userId = validated.userId, userLogin = validated.login))
                    _uiState.update { it.copy(authenticated = true, userLogin = validated.login) }
                    loadProfile(session.accessToken, validated.userId)
                }
                is ResponseState.Error -> logout()
                is ResponseState.Loading -> Unit
            }
        }
    }

    private suspend fun validateAndSave(token: String) {
        _uiState.update { it.copy(loading = true) }
        getValidTokenUseCase(token).collect { response ->
            when (response) {
                is ResponseState.Success -> {
                    val value = response.data ?: return@collect
                    tokenStore.write(AuthSession(token, value.userId, value.login))
                    clearOAuthState()
                    _uiState.update { it.copy(loading = false, authenticated = true, userLogin = value.login, oauthState = null) }
                    loadProfile(token, value.userId)
                }
                is ResponseState.Error -> _uiState.update { it.copy(loading = false, errorMessage = response.message ?: "Twitch sign-in failed") }
                is ResponseState.Loading -> Unit
            }
        }
    }

    private fun clearOAuthState() {
        savedStateHandle.remove<String>(OAUTH_STATE_KEY)
        _uiState.update { it.copy(oauthState = null) }
    }

    private suspend fun loadProfile(token: String, userId: String) {
        runCatching {
            twitchRepository.getTwitchUser(
                userId = userId,
                login = null,
                authorization = "Bearer $token",
                clientId = Constants.TWITCH_CLIENT_ID
            ).users.firstOrNull()?.profileImageUrl
        }.onSuccess { profileImageUrl ->
            _uiState.update { it.copy(profileImageUrl = profileImageUrl?.takeIf(String::isNotBlank)) }
        }
    }

    private suspend fun revokeToken(token: String) = withContext(Dispatchers.IO) {
        if (Constants.TWITCH_CLIENT_ID.isBlank()) return@withContext
        runCatching {
            val body = FormBody.Builder()
                .add("client_id", Constants.TWITCH_CLIENT_ID)
                .add("token", token)
                .build()
            val request = Request.Builder()
                .url("https://id.twitch.tv/oauth2/revoke")
                .post(body)
                .build()
            okHttpClient.newCall(request).execute().use { Unit }
        }
    }

    private fun demoItems(parentId: String): List<MediaItem> = List(8) { index ->
        val category = parentId == CustomMediaLibrarySessionCallback.TOP_GAMES
        MediaItem.Builder().setMediaId(if (category) "demo-game-$index" else "demo-stream-$index")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(if (category) listOf("Strategy", "Adventure", "Creative", "Speedrunning")[index % 4] else "Live audio session ${index + 1}")
                    .setArtist(if (category) "${12 + index}K listeners" else "Creator ${index + 1}")
                    .setAlbumTitle(if (category) null else "Demo category")
                    .setIsBrowsable(category)
                    .setIsPlayable(!category)
                    .build()
            ).build()
    }

    private companion object {
        const val OAUTH_STATE_KEY = "oauth_state"
        const val PAGE_SIZE = 50
    }
}
