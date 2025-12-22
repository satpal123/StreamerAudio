package com.player.streameraudio.data.api

import com.player.streameraudio.data.datasource.dto.TwitchSearchChannelsResponse
import com.player.streameraudio.data.datasource.dto.TwitchStreamersResponse
import com.player.streameraudio.data.datasource.dto.TwitchTopGamesResponse
import com.player.streameraudio.data.datasource.dto.TwitchUserResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TwitchApiService {

    //Get User Details
    @GET("users")
    suspend fun getTwitchUser(
        @Query("id") userId: String?,
        @Query("login") login: String?,
        @Header("Authorization") authorization: String,
        @Header("Client-Id") clientId: String
    ): TwitchUserResponse

    @GET("streams")
    suspend fun getStreamsInfo(
        @Query("user_id") userId: String?,
        @Header("Authorization") authorization: String,
        @Header("Client-Id") clientId: String
    ): TwitchStreamersResponse


    //Get Logged in User Followed Streamers Online
    @GET("streams/followed")
    suspend fun getTwitchStreams(
        @Query("user_id") userId: String?,
        @Header("Authorization") authorization: String,
        @Header("Client-Id") clientId: String,
        @Query("after") after: String? = null,
        @Query("first") first: Int = 50
    ): TwitchStreamersResponse

    // Top Streams
    @GET("streams")
    suspend fun getTopStreams(
        @Header("Authorization") authorization: String,
        @Header("Client-Id") clientId: String,
        @Query("first") first: Int = 50,
        @Query("after") after: String? = null
    ): TwitchStreamersResponse

    // Streams by Game
    @GET("streams")
    suspend fun getStreamsByGame(
        @Header("Authorization") authorization: String,
        @Header("Client-Id") clientId: String,
        @Query("game_id") gameId: String,
        @Query("first") first: Int = 50,
        @Query("after") after: String? = null
    ): TwitchStreamersResponse

    // Top Games
    @GET("games/top")
    suspend fun getTopGames(
        @Header("Authorization") authorization: String,
        @Header("Client-Id") clientId: String,
        @Query("first") first: Int = 50,
        @Query("after") after: String? = null
    ): TwitchTopGamesResponse

    // Search Channels
    @GET("search/channels")
    suspend fun searchChannels(
        @Header("Authorization") authorization: String,
        @Header("Client-Id") clientId: String,
        @Query("query") query: String,
        @Query("live_only") liveOnly: Boolean = true,
        @Query("first") first: Int = 50,
        @Query("after") after: String? = null
    ): TwitchSearchChannelsResponse
}