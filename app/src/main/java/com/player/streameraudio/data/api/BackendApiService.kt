package com.player.streameraudio.data.api

import com.player.streameraudio.data.datasource.dto.BackendCategoriesResponse
import com.player.streameraudio.data.datasource.dto.BackendChannelsResponse
import com.player.streameraudio.data.datasource.dto.BackendProfileImageResponse
import com.player.streameraudio.data.datasource.dto.BackendTopChannelsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BackendApiService {

    @GET("categories")
    suspend fun getCategories(
        @Query("first") first: Int = 50,
        @Query("cursor") cursor: String? = null
    ): BackendCategoriesResponse

    @GET("channels")
    suspend fun getChannels(
        @Query("game_id") gameId: String,
        @Query("first") first: Int = 50,
        @Query("cursor") cursor: String? = null
    ): BackendChannelsResponse

    @GET("top-streams")
    suspend fun getTopStreams(
        @Query("first") first: Int = 50,
        @Query("cursor") cursor: String? = null
    ): BackendTopChannelsResponse

    @GET("profile-image")
    suspend fun getProfileImage(
        @Query("login") login: String
    ): BackendProfileImageResponse
}