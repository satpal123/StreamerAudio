package com.player.streameraudio.domain.usecase

import com.player.streameraudio.data.datasource.dto.BackendChannelDTO
import com.player.streameraudio.data.util.ResponseState
import com.player.streameraudio.domain.repository.BackendRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class GetBackendChannelsUseCase @Inject constructor(
    private val repository: BackendRepository
) {
    operator fun invoke(
        gameId: String,
        cursor: String? = null
    ): Flow<ResponseState<Pair<List<BackendChannelDTO>, String?>>> = flow {
        try {
            emit(ResponseState.Loading())
            val response = repository.getChannels(gameId, cursor)
            val channels = response.channels
            val nextCursor = response.pagination?.cursor
            emit(ResponseState.Success(Pair(channels, nextCursor)))
        } catch (e: HttpException) {
            emit(ResponseState.Error(e.localizedMessage ?: "An Unexpected Error"))
        } catch (e: IOException) {
            emit(ResponseState.Error("Internet Error"))
        }
    }
}