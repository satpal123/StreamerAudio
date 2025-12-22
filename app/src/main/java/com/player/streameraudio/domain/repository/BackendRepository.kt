package com.player.streameraudio.domain.repository

import com.player.streameraudio.data.datasource.dto.BackendCategoriesResponse
import com.player.streameraudio.data.datasource.dto.BackendChannelsResponse
import com.player.streameraudio.data.datasource.dto.BackendProfileImageResponse
import com.player.streameraudio.data.datasource.dto.BackendTopChannelsResponse

interface BackendRepository {
    suspend fun getCategories(cursor: String? = null): BackendCategoriesResponse
    suspend fun getChannels(gameId: String, cursor: String? = null): BackendChannelsResponse
    suspend fun getTopStreams(cursor: String? = null): BackendTopChannelsResponse
    suspend fun getProfileImage(login: String): BackendProfileImageResponse
}