package com.player.streameraudio.domain.usecase

import com.player.streameraudio.data.datasource.dto.TwitchStream
import com.player.streameraudio.data.util.ResponseState
import com.player.streameraudio.domain.repository.TwitchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class GetTwitchStreamsDetailsUseCase @Inject constructor(
    private val repository: TwitchRepository
) {
    operator fun invoke(userId: String?,
                         authorization: String,
                         clientId: String): Flow<ResponseState<List<TwitchStream>>> = flow {

        try {

            emit(ResponseState.Loading<List<TwitchStream>>())
            val twitchStreamersList = repository.getStreamsInfo(userId, authorization, clientId)
                .streamers.map {
                it
            }

            emit(ResponseState.Success<List<TwitchStream>>(twitchStreamersList))
        } catch (e: HttpException) {
            emit(ResponseState.Error<List<TwitchStream>>(e.localizedMessage ?: "An Unexpected Error"))
        } catch (e: IOException) {
            emit(ResponseState.Error<List<TwitchStream>>("Internet Error"))
        }
    }
}