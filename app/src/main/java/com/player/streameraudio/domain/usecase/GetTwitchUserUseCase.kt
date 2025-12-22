package com.player.streameraudio.domain.usecase

import com.player.streameraudio.data.datasource.dto.TwitchUser
import com.player.streameraudio.data.util.ResponseState
import com.player.streameraudio.domain.repository.TwitchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class GetTwitchUserUseCase @Inject constructor(
    private val repository: TwitchRepository
) {
    operator fun invoke(userId: String?,
                        login: String?,
                        authorization: String,
                        clientId: String): Flow<ResponseState<List<TwitchUser>>> = flow {

        try {
            emit(ResponseState.Loading<List<TwitchUser>>())
            val twitchStreamers = repository.getTwitchUser(userId, login, authorization, clientId).users.map {
                it
            }

            emit(ResponseState.Success<List<TwitchUser>>(twitchStreamers))
        } catch (e: HttpException) {
            emit(ResponseState.Error<List<TwitchUser>>(e.localizedMessage ?: "An Unexpected Error"))
        } catch (e: IOException) {
            emit(ResponseState.Error<List<TwitchUser>>("Internet Error"))
        }
    }
}