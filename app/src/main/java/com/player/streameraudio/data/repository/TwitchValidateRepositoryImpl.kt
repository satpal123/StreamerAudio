package com.player.streameraudio.data.repository

import com.player.streameraudio.data.api.TwitchOauthApiService
import com.player.streameraudio.data.datasource.dto.TwitchValidationDTO
import com.player.streameraudio.domain.repository.TwitchValidateRepository
import javax.inject.Inject

class TwitchValidateRepositoryImpl @Inject constructor(
    private val twitchOauthApiService: TwitchOauthApiService) : TwitchValidateRepository {
    override suspend fun validateAccessToken(
        accessToken: String
    ): TwitchValidationDTO {
        val token = accessToken.trim()
            .removePrefix("OAuth ")
            .removePrefix("Bearer ")
            .trim()
        require(token.isNotEmpty()) { "Access token is empty" }
        val authorization = "OAuth $token"
        return twitchOauthApiService.validateAccessToken(authorization)
    }
}
