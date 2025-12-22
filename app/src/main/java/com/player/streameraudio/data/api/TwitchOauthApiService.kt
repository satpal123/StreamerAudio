package com.player.streameraudio.data.api

import com.player.streameraudio.data.datasource.dto.TwitchValidationDTO
import retrofit2.http.GET
import retrofit2.http.Header

interface TwitchOauthApiService {
    //Validate Access Token
    @GET("oauth2/validate")
    suspend fun validateAccessToken(
        @Header("Authorization") accessToken: String
    ): TwitchValidationDTO
}