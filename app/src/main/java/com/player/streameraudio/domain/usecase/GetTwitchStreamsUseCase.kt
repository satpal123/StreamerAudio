package com.player.streameraudio.domain.usecase

import com.player.streameraudio.data.datasource.dto.TwitchStream
import com.player.streameraudio.data.util.ResponseState
import com.player.streameraudio.domain.repository.TwitchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class GetTwitchStreamsUseCase @Inject constructor(
    private val repository: TwitchRepository
) {
    operator fun invoke(
        userId: String?,
        authorization: String,
        clientId: String,
        after: String? = null
    ): Flow<ResponseState<Pair<List<TwitchStream>, String?>>> = flow {

        try {

            emit(ResponseState.Loading<Pair<List<TwitchStream>, String?>>())
            val response = repository.getTwitchStreams(userId, authorization, clientId, after)
            val twitchStreamersList = response.streamers.map { it }
            val nextCursor = response.pagination?.cursor

            emit(ResponseState.Success<Pair<List<TwitchStream>, String?>>(Pair(twitchStreamersList, nextCursor)))
        } catch (e: HttpException) {
            emit(ResponseState.Error<Pair<List<TwitchStream>, String?>>(e.localizedMessage ?: "An Unexpected Error"))
        } catch (e: IOException) {
            emit(ResponseState.Error<Pair<List<TwitchStream>, String?>>("Internet Error"))
        }
    }
}