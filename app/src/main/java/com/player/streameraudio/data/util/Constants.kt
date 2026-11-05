package com.player.streameraudio.data.util

object Constants {
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    const val TWITCH_GQL_ENDPOINT = "https://gql.twitch.tv/gql"
    const val TWITCH_OAUTH_ENDPOINT = "https://id.twitch.tv/oauth2/authorize"
    const val TWITCH_OAUTH_VALIDATE_ENDPOINT = "https://id.twitch.tv/"
    const val TWITCH_HLS_BASE = "https://usher.ttvnw.net/api/channel/hls/"
    val TWITCH_REDIRECT_URL: String = com.player.streameraudio.BuildConfig.TWITCH_REDIRECT_URL
    val TWITCH_OAUTH_SCOPES = arrayOf("user:read:email", "user:read:follows")
    val TWITCH_CLIENT_ID: String = com.player.streameraudio.BuildConfig.TWITCH_CLIENT_ID
    const val TWITCH_GRAPHQL_CLIENT_ID = "kimne78kx3ncx6brgo4mv6wki5h1ko"
    const val TWITCH_BASE_URL = "https://api.twitch.tv/helix/"
    const val BACKEND_BASE_URL = "https://streameraudio-twitch-backend.netlify.app/.netlify/functions/"
    const val ACCESS_TOKEN_KEY = "access_token"
    const val LOGGED_IN_USERID = "user_id"

    // Legacy SharedPreferences keys used only by the one-time v2 migration.
    const val PREF_ACCESS_TOKEN = "access_token"
    const val PREF_USER_ID = "user_id"
    const val PREF_USER_LOGIN = "user_login"
    const val PREF_SELECTED_PROXY = "selected_proxy"
    const val PREF_STREAM_SOURCE = "stream_source"
}
