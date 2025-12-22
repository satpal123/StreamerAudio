package com.player.streameraudio.domain.repository

import com.player.streameraudio.data.datasource.dto.TwitchSearchChannelsResponse
import com.player.streameraudio.data.datasource.dto.TwitchStreamersResponse
import com.player.streameraudio.data.datasource.dto.TwitchTopGamesResponse
import com.player.streameraudio.data.datasource.dto.TwitchUserResponse

interface TwitchRepository {

    suspend fun getTwitchUser(
        userId: String?,
        login: String?,
        authorization: String,
        clientId: String
    ): TwitchUserResponse

    suspend fun getTwitchStreams(
        userId: String?,
        authorization: String,
        clientId: String,
        after: String? = null
    ): TwitchStreamersResponse

    suspend fun getStreamsInfo(
        userId: String?,
        authorization: String,
        clientId: String
    ): TwitchStreamersResponse

    // Explore (post-login)
    suspend fun getTopStreams(
        authorization: String,
        clientId: String,
        after: String? = null
    ): TwitchStreamersResponse

    suspend fun getStreamsByGame(
        authorization: String,
        clientId: String,
        gameId: String,
        after: String? = null
    ): TwitchStreamersResponse

    suspend fun getTopGames(
        authorization: String,
        clientId: String,
        after: String? = null
    ): TwitchTopGamesResponse

    suspend fun searchChannels(
        authorization: String,
        clientId: String,
        query: String,
        after: String? = null
    ): TwitchSearchChannelsResponse
}