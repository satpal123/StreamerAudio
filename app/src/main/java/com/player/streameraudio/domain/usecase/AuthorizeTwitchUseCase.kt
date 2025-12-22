package com.player.streameraudio.domain.usecase

import com.player.streameraudio.data.datasource.dto.TwitchAccessTokenDTO
import com.player.streameraudio.data.util.ResponseState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

object AuthorizeTwitchUseCase {
    fun handleOAuthResponse(
        params: Map<String, String>
    ): Flow<ResponseState<TwitchAccessTokenDTO>> = flow {

        try {
            val accessToken = params["access_token"] ?: throw IllegalArgumentException("Missing access_token")
            val scope = params["scope"] ?: throw IllegalArgumentException("Missing scope")
            val tokenType = params["token_type"] ?: throw IllegalArgumentException("Missing token_type")

            emit(ResponseState.Loading<TwitchAccessTokenDTO>())

            emit(ResponseState.Success<TwitchAccessTokenDTO>(TwitchAccessTokenDTO(accessToken, scope, tokenType)))

        } catch (e: HttpException) {
            emit(ResponseState.Error<TwitchAccessTokenDTO>(e.localizedMessage ?: "An Unexpected Error"))
        } catch (e: IOException) {
            emit(ResponseState.Error<TwitchAccessTokenDTO>("Internet Error"))
        }
    }
}