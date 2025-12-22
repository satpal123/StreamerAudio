package com.player.streameraudio.domain.usecase

import com.player.streameraudio.data.datasource.dto.BackendUserDTO
import com.player.streameraudio.data.util.ResponseState
import com.player.streameraudio.domain.repository.BackendRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class GetBackendProfileImageUseCase @Inject constructor(
    private val repository: BackendRepository
) {
    operator fun invoke(
        login: String
    ): Flow<ResponseState<BackendUserDTO?>> = flow {
        try {
            emit(ResponseState.Loading())
            val response = repository.getProfileImage(login)
            val user = response.data.firstOrNull()
            emit(ResponseState.Success(user))
        } catch (e: HttpException) {
            emit(ResponseState.Error(e.localizedMessage ?: "An Unexpected Error"))
        } catch (e: IOException) {
            emit(ResponseState.Error("Internet Error"))
        }
    }
}