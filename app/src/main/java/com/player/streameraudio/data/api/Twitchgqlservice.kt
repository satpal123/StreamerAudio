package com.player.streameraudio.data.api

import android.util.Log
import com.player.streameraudio.data.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for fetching Twitch stream access tokens via GraphQL
 * Required for direct HLS playback
 */
@Singleton
class TwitchGqlService @Inject constructor(
    private val client: OkHttpClient
) {

    private val tag = "TwitchGqlService"

    @Volatile
    private var cachedIntegrityToken: CachedIntegrityToken? = null

    // Twitch internal endpoint for integrity tokens
    private val integrityUrl = "https://gql.twitch.tv/integrity"

    // Add browser-ish headers (helps with Twitch tightening checks)
    private val origin = "https://www.twitch.tv"
    private val referer = "https://www.twitch.tv/"

    // Stable per app-process run (don’t use a constant)
    private val deviceId: String = UUID.randomUUID().toString().replace("-", "")

    // Full query to avoid persistedQuery hash changes breaking playback.
    // Only declare variables we actually use (GraphQL will error on unused variables).
    private val playbackAccessTokenQuery = """
        query PlaybackAccessToken(${"$"}login: String!, ${"$"}playerType: String!) {
          streamPlaybackAccessToken(
            channelName: ${"$"}login,
            params: { platform: "web", playerBackend: "mediaplayer", playerType: ${"$"}playerType }
          ) {
            value
            signature
          }
        }
    """

    private fun Request.Builder.addCommonHeaders(
        clientId: String,
        accessToken: String? = null
    ): Request.Builder {
        val builder = this
            .header("Client-Id", clientId)
            .header("X-Device-Id", deviceId)
            .header("User-Agent", Constants.USER_AGENT)
            .header("Origin", origin)
            .header("Referer", referer)
            .header("Accept", "application/json")
        if (!accessToken.isNullOrBlank()) {
            builder.header("Authorization", "OAuth $accessToken")
        }
        return builder
    }

    /**
     * Fetches a new Integrity Token from Twitch.
     * This proves the request is coming from a "valid" client.
     */
    private suspend fun fetchIntegrityToken(
        clientId: String,
        accessToken: String?
    ): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(integrityUrl)
                .post("{}".toRequestBody("application/json".toMediaType()))
                .addCommonHeaders(clientId, accessToken)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(tag, "Integrity token fetch failed: ${response.code}")
                    return@withContext null
                }
                val token = JSONObject(response.body.string()).optString("token").takeIf { it.isNotBlank() }
                if (token != null) {
                    cachedIntegrityToken = CachedIntegrityToken(clientId, accessToken?.hashCode(), token)
                    return@withContext token
                }
            }
            Log.w(tag, "Integrity token missing in response")
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch integrity token", e)
        }
        null
    }

    /**
     * Fetch playback access token and signature for a channel
     * @param channelName The streamer's login name
     * @param clientId Twitch Client ID
     * @return Pair of (token, signature) or null if failed
     */
    suspend fun getPlaybackAccessToken(
        channelName: String,
        clientId: String,
        accessToken: String? = null
    ): Pair<String, String>? = withContext(Dispatchers.IO) {

        // First try with cached/new integrity token
        val cached = cachedIntegrityToken?.takeIf {
            it.clientId == clientId && it.authorizationHash == accessToken?.hashCode()
        }?.token
        val firstIntegrity = cached ?: fetchIntegrityToken(clientId, accessToken)
        val first = getPlaybackAccessTokenOnce(channelName, clientId, accessToken, firstIntegrity)
        if (first != null) return@withContext first

        // If it failed, clear integrity and retry once (often fixes "stale/invalid integrity")
        cachedIntegrityToken = null
        val secondIntegrity = fetchIntegrityToken(clientId, accessToken)
        return@withContext getPlaybackAccessTokenOnce(
            channelName,
            clientId,
            accessToken,
            secondIntegrity
        )
    }

    private fun getPlaybackAccessTokenOnce(
        channelName: String,
        clientId: String,
        accessToken: String?,
        integrityToken: String?
    ): Pair<String, String>? {
        return try {
            val payload = JSONObject().apply {
                put("operationName", "PlaybackAccessToken")
                put("query", playbackAccessTokenQuery.trimIndent())
                put("variables", JSONObject().apply {
                    put("login", channelName.lowercase())
                    // "site" tends to behave more like normal web playback.
                    // If Twitch changes behaviour, you can try "embed" again.
                    put("playerType", "site")
                })
            }

            val requestBuilder = Request.Builder()
                .url(Constants.TWITCH_GQL_ENDPOINT)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .addCommonHeaders(clientId, accessToken)

            // Attach integrity if we have it
            if (!integrityToken.isNullOrBlank()) {
                requestBuilder.header("Client-Integrity", integrityToken)
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(tag, "Playback token request failed: ${response.code}")
                    return null
                }
                val json = JSONObject(response.body.string())
                if ((json.optJSONArray("errors")?.length() ?: 0) > 0) {
                    Log.w(tag, "Playback token GraphQL response contained errors")
                }
                val accessToken = json.optJSONObject("data")
                    ?.optJSONObject("streamPlaybackAccessToken")
                val token = accessToken?.optString("value")
                val signature = accessToken?.optString("signature")
                if (token.isNullOrBlank() || signature.isNullOrBlank()) {
                    Log.w(tag, "Playback token missing or blank")
                    return null
                }
                Pair(token, signature)
            }
        } catch (e: Exception) {
            Log.e(tag, "Playback token request exception", e)
            null
        }
    }

    /**
     * Build HLS master playlist URL with token and signature
     * @param channelName The streamer's login name
     * @param token Playback access token
     * @param signature Token signature
     * @return Complete HLS URL
     */
    fun buildHlsUrl(channelName: String, token: String, signature: String): String {
        val encodedToken = java.net.URLEncoder.encode(token, "UTF-8")
        val random = (Math.random() * 999999).toInt()

        return "${Constants.TWITCH_HLS_BASE}${channelName.lowercase()}.m3u8" +
                "?sig=$signature" +
                "&token=$encodedToken" +
                "&allow_source=true" +
                "&allow_audio_only=true" +
                "&fast_bread=false" +
                "&p=$random"
    }

    private data class CachedIntegrityToken(
        val clientId: String,
        val authorizationHash: Int?,
        val token: String
    )
}
