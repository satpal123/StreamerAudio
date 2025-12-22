package com.player.streameraudio.domain.usecase

import com.player.streameraudio.data.datasource.dto.TwitchGameDTO
import com.player.streameraudio.data.util.ResponseState
import com.player.streameraudio.domain.repository.TwitchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class GetTopGamesUseCase @Inject constructor(
    private val repository: TwitchRepository
) {
    operator fun invoke(
        authorization: String,
        clientId: String,
        after: String? = null
    ): Flow<ResponseState<Pair<List<TwitchGameDTO>, String?>>> = flow {
        try {
            emit(ResponseState.Loading())
            val response = repository.getTopGames(authorization, clientId, after)
            val games = response.data
            val nextCursor = response.pagination?.cursor
            emit(ResponseState.Success(Pair(games, nextCursor)))
        } catch (e: HttpException) {
            emit(ResponseState.Error(e.localizedMessage ?: "An Unexpected Error"))
        } catch (e: IOException) {
            emit(ResponseState.Error("Internet Error"))
        }
    }
}