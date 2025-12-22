package com.player.streameraudio.data.repository

import com.player.streameraudio.data.api.BackendApiService
import com.player.streameraudio.data.datasource.dto.BackendCategoriesResponse
import com.player.streameraudio.data.datasource.dto.BackendChannelsResponse
import com.player.streameraudio.data.datasource.dto.BackendProfileImageResponse
import com.player.streameraudio.data.datasource.dto.BackendTopChannelsResponse
import com.player.streameraudio.domain.repository.BackendRepository
import javax.inject.Inject

class BackendRepositoryImpl @Inject constructor(
    private val backendApiService: BackendApiService
) : BackendRepository {

    override suspend fun getCategories(cursor: String?): BackendCategoriesResponse {
        return backendApiService.getCategories(first = 50, cursor = cursor)
    }

    override suspend fun getChannels(gameId: String, cursor: String?): BackendChannelsResponse {
        return backendApiService.getChannels(gameId = gameId, first = 50, cursor = cursor)
    }

    override suspend fun getTopStreams(cursor: String?): BackendTopChannelsResponse {
        return backendApiService.getTopStreams(first = 50, cursor = cursor)
    }

    override suspend fun getProfileImage(login: String): BackendProfileImageResponse {
        return backendApiService.getProfileImage(login = login)
    }
}