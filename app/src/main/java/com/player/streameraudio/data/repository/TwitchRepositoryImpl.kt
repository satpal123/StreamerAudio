package com.player.streameraudio.data.repository

import com.player.streameraudio.data.api.TwitchApiService
import com.player.streameraudio.data.datasource.dto.TwitchSearchChannelsResponse
import com.player.streameraudio.data.datasource.dto.TwitchStreamersResponse
import com.player.streameraudio.data.datasource.dto.TwitchTopGamesResponse
import com.player.streameraudio.data.datasource.dto.TwitchUserResponse
import com.player.streameraudio.domain.repository.TwitchRepository
import javax.inject.Inject

class TwitchRepositoryImpl @Inject constructor(
    private val twitchApiService: TwitchApiService) : TwitchRepository {

    override suspend fun getTwitchUser(
        userId: String?,
        login: String?,
        authorization: String,
        clientId: String
    ): TwitchUserResponse {
        return twitchApiService.getTwitchUser(userId, login, authorization, clientId)
    }

    override suspend fun getTwitchStreams(
        userId: String?,
        authorization: String,
        clientId: String,
        after: String?
    ): TwitchStreamersResponse {
        return twitchApiService.getTwitchStreams(userId, authorization, clientId, after)
    }

    override suspend fun getStreamsInfo(
        userId: String?,
        authorization: String,
        clientId: String
    ): TwitchStreamersResponse {
        return twitchApiService.getStreamsInfo(userId, authorization, clientId)
    }

    override suspend fun getTopStreams(
        authorization: String,
        clientId: String,
        after: String?
    ): TwitchStreamersResponse{
        return twitchApiService.getTopStreams(authorization, clientId, after = after)
    }

    override suspend fun getStreamsByGame(
        authorization: String,
        clientId: String,
        gameId: String,
        after: String?
    ): TwitchStreamersResponse{
        return twitchApiService.getStreamsByGame(authorization, clientId, gameId, after = after)
    }

    override suspend fun getTopGames(
        authorization: String,
        clientId: String,
        after: String?
    ): TwitchTopGamesResponse{
        return twitchApiService.getTopGames(authorization, clientId, after = after)
    }

    override suspend fun searchChannels(
        authorization: String,
        clientId: String,
        query: String,
        after: String?
    ): TwitchSearchChannelsResponse{
        return twitchApiService.searchChannels(authorization, clientId, query, after = after)
    }

}