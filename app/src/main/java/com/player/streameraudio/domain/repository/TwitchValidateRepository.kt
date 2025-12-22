package com.player.streameraudio.domain.repository

import com.player.streameraudio.data.datasource.dto.TwitchValidationDTO

interface TwitchValidateRepository {
    suspend fun validateAccessToken(
        accessToken: String
    ): TwitchValidationDTO
}