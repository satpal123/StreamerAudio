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
import java.util.concurrent.TimeUnit

/**
 * Service for fetching Twitch stream access tokens via GraphQL
 * Required for direct HLS playback
 */
object TwitchGqlService {

    private const val TAG = "TwitchGqlService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var cachedIntegrityToken: String? = null

    // Twitch internal endpoint for integrity tokens
    private const val INTEGRITY_URL = "https://gql.twitch.tv/integrity"

    // Add browser-ish headers (helps with Twitch tightening checks)
    private const val ORIGIN = "https://www.twitch.tv"
    private const val REFERER = "https://www.twitch.tv/"

    // Stable per app-process run (don’t use a constant)
    private val deviceId: String = UUID.randomUUID().toString().replace("-", "")

    // Full query to avoid persistedQuery hash changes breaking playback.
    // Only declare variables we actually use (GraphQL will error on unused variables).
    private const val PLAYBACK_ACCESS_TOKEN_QUERY = """
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

    private fun Request.Builder.addCommonHeaders(clientId: String): Request.Builder {
        return this
            .header("Client-Id", clientId)
            .header("X-Device-Id", deviceId)
            .header("User-Agent", Constants.USER_AGENT)
            .header("Origin", ORIGIN)
            .header("Referer", REFERER)
            .header("Accept", "application/json")
    }

    /**
     * Fetches a new Integrity Token from Twitch.
     * This proves the request is coming from a "valid" client.
     */
    private suspend fun fetchIntegrityToken(clientId: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(INTEGRITY_URL)
                .post("{}".toRequestBody("application/json".toMediaType()))
                .addCommonHeaders(clientId)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.w(TAG, "Integrity token fetch failed: ${response.code} body=$body")
                return@withContext null
            }

            val json = JSONObject(body)
            val token = json.optString("token").takeIf { it.isNotBlank() }

            if (token != null) {
                cachedIntegrityToken = token
                return@withContext token
            }

            Log.w(TAG, "Integrity token missing in response body=$body")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch integrity token", e)
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
        clientId: String
    ): Pair<String, String>? = withContext(Dispatchers.IO) {

        // First try with cached/new integrity token
        val firstIntegrity = cachedIntegrityToken ?: fetchIntegrityToken(clientId)
        val first = getPlaybackAccessTokenOnce(channelName, clientId, firstIntegrity)
        if (first != null) return@withContext first

        // If it failed, clear integrity and retry once (often fixes "stale/invalid integrity")
        cachedIntegrityToken = null
        val secondIntegrity = fetchIntegrityToken(clientId)
        return@withContext getPlaybackAccessTokenOnce(channelName, clientId, secondIntegrity)
    }

    private fun getPlaybackAccessTokenOnce(
        channelName: String,
        clientId: String,
        integrityToken: String?
    ): Pair<String, String>? {
        return try {
            val payload = JSONObject().apply {
                put("operationName", "PlaybackAccessToken")
                put("query", PLAYBACK_ACCESS_TOKEN_QUERY.trimIndent())
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
                .addCommonHeaders(clientId)

            // Attach integrity if we have it
            if (!integrityToken.isNullOrBlank()) {
                requestBuilder.header("Client-Integrity", integrityToken)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.w(TAG, "Playback token request failed: ${response.code} body=$responseBody")
                return null
            }

            val json = JSONObject(responseBody)

            // If Twitch returns errors, log them and fail gracefully
            json.optJSONArray("errors")?.let { errs ->
                if (errs.length() > 0) {
                    Log.w(TAG, "Playback token GraphQL errors: $errs")
                }
            }

            val data = json.optJSONObject("data")
            val streamPlaybackAccessToken = data?.optJSONObject("streamPlaybackAccessToken")

            val token = streamPlaybackAccessToken?.optString("value")
            val signature = streamPlaybackAccessToken?.optString("signature")

            if (token.isNullOrBlank() || signature.isNullOrBlank()) {
                Log.w(TAG, "Playback token missing/blank. body=$responseBody")
                return null
            }

            Pair(token, signature)
        } catch (e: Exception) {
            Log.e(TAG, "Playback token request exception", e)
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
}
