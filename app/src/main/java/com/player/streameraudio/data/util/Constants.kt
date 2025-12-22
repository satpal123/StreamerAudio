package com.player.streameraudio.data.util

object Constants {
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    const val TWITCH_GQL_ENDPOINT = "https://gql.twitch.tv/gql"
    const val TWITCH_OAUTH_ENDPOINT = "https://id.twitch.tv/oauth2/authorize"
    const val TWITCH_OAUTH_VALIDATE_ENDPOINT = "https://id.twitch.tv/"
    const val TWITCH_HLS_BASE = "https://usher.ttvnw.net/api/channel/hls/"
    const val TWITCH_REDIRECT_URL = "http://localhost"
    val TWITCH_OAUTH_SCOPES = arrayOf("user:read:email", "user:read:follows")
    const val TWITCH_CLIENT_ID = "YOUR_TWITCH_CLIENT_ID_HERE"
    const val TWITCH_GRAPHQL_CLIENT_ID = "kimne78kx3ncx6brgo4mv6wki5h1ko"
    const val TWITCH_BASE_URL = "https://api.twitch.tv/helix/"
    const val BACKEND_BASE_URL = "https://streameraudio-twitch-backend.netlify.app/.netlify/functions/"
    const val ACCESS_TOKEN_KEY = "access_token"
    const val LOGGED_IN_USERID = "user_id"

    // Luminous Proxy URLs
    const val PROXY_EU = "https://eu.luminous.dev"
    const val PROXY_EU2 = "https://eu2.luminous.dev"
    const val PROXY_EU3 = "https://eu3.luminous.dev"
    const val PROXY_AS = "https://as.luminous.dev"

    // Default proxy (deprecated - use getProxyUrl instead)
    @Deprecated("Use getProxyUrl() to get the selected proxy")
    const val PROXY_URL = "https://as.luminous.dev"

    // SharedPreferences keys
    const val PREF_ACCESS_TOKEN = "access_token"
    const val PREF_USER_ID = "user_id"
    const val PREF_USER_LOGIN = "user_login"
    const val PREF_SELECTED_PROXY = "selected_proxy"
    const val PREF_STREAM_SOURCE = "stream_source"  // "HLS" or "PROXY"

    // Stream source options
    const val STREAM_SOURCE_HLS = "HLS"
    const val STREAM_SOURCE_PROXY = "PROXY"

    // Get current proxy URL from SharedPreferences
    fun getProxyUrl(context: android.content.Context): String {
        val prefs = SecurePreferences.getEncryptedSharedPreferences(context)
        return prefs.getString(PREF_SELECTED_PROXY, PROXY_AS) ?: PROXY_AS
    }

    // Get current stream source preference (HLS or PROXY)
    fun getStreamSource(context: android.content.Context): String {
        val prefs = SecurePreferences.getEncryptedSharedPreferences(context)
        return prefs.getString(PREF_STREAM_SOURCE, STREAM_SOURCE_PROXY) ?: STREAM_SOURCE_PROXY
    }

    // Set stream source preference
    fun setStreamSource(context: android.content.Context, source: String) {
        val prefs = SecurePreferences.getEncryptedSharedPreferences(context)
        prefs.edit().putString(PREF_STREAM_SOURCE, source).apply()
    }
}