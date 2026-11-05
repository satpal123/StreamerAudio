package com.player.streameraudio.data.playback

import android.util.Log
import com.player.streameraudio.data.api.TwitchGqlService
import com.player.streameraudio.data.auth.TokenStore
import com.player.streameraudio.data.settings.PlaybackMode
import com.player.streameraudio.data.util.Constants
import com.player.streameraudio.data.util.ExtM3UParser
import com.player.streameraudio.domain.repository.PlaybackSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamResolver @Inject constructor(
    private val twitchGqlService: TwitchGqlService,
    private val tokenStore: TokenStore,
    private val settingsRepository: PlaybackSettingsRepository,
    private val okHttpClient: OkHttpClient
) {
    suspend fun resolve(channelLogin: String): String? {
        val settings = settingsRepository.settings.value
        val proxyUrl = settings.routing.selectedProxyId
            ?.let { selected -> settings.proxies.firstOrNull { it.id == selected }?.baseUrl }
        val direct: suspend () -> String? = { direct(channelLogin) }
        val proxy: suspend () -> String? = { proxyUrl?.let { proxy(channelLogin, it) } }

        return when (settings.routing.mode) {
            PlaybackMode.DIRECT -> direct() ?: if (settings.routing.fallbackEnabled) proxy() else null
            PlaybackMode.PROXY -> proxy() ?: if (settings.routing.fallbackEnabled) direct() else null
        }
    }

    private suspend fun direct(channelLogin: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val normalized = channelLogin.trim().lowercase()
            val userToken = tokenStore.read()?.accessToken?.takeIf { it.isNotBlank() }
            val authenticatedToken = userToken?.let { accessToken ->
                Constants.TWITCH_CLIENT_ID.takeIf { it.isNotBlank() }?.let { clientId ->
                    twitchGqlService.getPlaybackAccessToken(
                        channelName = normalized,
                        clientId = clientId,
                        accessToken = accessToken
                    )
                }
            }
            val (token, signature) = authenticatedToken
                ?: twitchGqlService.getPlaybackAccessToken(
                    channelName = normalized,
                    clientId = Constants.TWITCH_GRAPHQL_CLIENT_ID
                )
                ?: return@withContext null
            val hlsUrl = twitchGqlService.buildHlsUrl(normalized, token, signature)
            parseAudioPlaylist(hlsUrl) ?: hlsUrl
        }.onFailure { Log.w(TAG, "Direct stream resolution failed") }.getOrNull()
    }

    private suspend fun parseAudioPlaylist(masterUrl: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(masterUrl).header("User-Agent", Constants.USER_AGENT).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                ExtM3UParser().parse(response.body.string())
                    .firstOrNull { it.name == "audio_only" }
                    ?.playlistUrl
            }
        }.getOrNull()
    }

    private suspend fun proxy(channelLogin: String, baseUrl: String): String? = withContext(Dispatchers.IO) {
        val safeLogin = channelLogin.trim().lowercase()
        for (fastBread in listOf(true, false)) {
            val url = baseUrl.toHttpUrl().newBuilder()
                .addPathSegment("live")
                .addPathSegment(safeLogin)
                .addQueryParameter("allow_source", "true")
                .addQueryParameter("allow_audio_only", "true")
                .addQueryParameter("fast_bread", fastBread.toString())
                .build()
            try {
                val request = Request.Builder().url(url).header("User-Agent", Constants.USER_AGENT).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Proxy request failed: ${response.code}")
                        return@use
                    }
                    val entries = ExtM3UParser().parse(response.body.string())
                    entries.firstOrNull { it.name == "audio_only" }?.playlistUrl?.let { return@withContext it }
                    entries.firstOrNull()?.playlistUrl?.let { return@withContext it }
                }
            } catch (_: Exception) {
                Log.w(TAG, "Proxy stream resolution failed")
            }
        }
        null
    }

    private companion object {
        const val TAG = "StreamResolver"
    }
}
