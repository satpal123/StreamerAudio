package com.player.streameraudio.domain.usecase

import com.player.streameraudio.data.datasource.dto.TwitchValidationDTO
import com.player.streameraudio.data.util.ResponseState
import com.player.streameraudio.domain.repository.TwitchValidateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import javax.inject.Inject

class GetValidTokenUseCase @Inject constructor(
    private val repository: TwitchValidateRepository
) {
    operator fun invoke(authorization: String): Flow<ResponseState<TwitchValidationDTO>> = flow {

        try {
            emit(ResponseState.Loading())
            val validateTokenResponse = repository.validateAccessToken(authorization)

            // Map the repository response to the TwitchValidationDTO data class
            val validationDTO = validateTokenResponse.let { response ->
                TwitchValidationDTO(
                    clientId = response.clientId,
                    login = response.login,
                    scopes = response.scopes,
                    userId = response.userId,
                    expiresIn = response.expiresIn
                )
            }

            emit(ResponseState.Success(validationDTO))
        } catch (e: IOException) {
            emit(ResponseState.Error("Internet Error"))
        } catch (e: Exception) {
            // Handle different error cases
            val errorMessage = when {
                e.message?.contains("401") == true -> "invalid access token"
                else -> "Unknown error occurred: ${e.localizedMessage}"
            }
            emit(ResponseState.Error(errorMessage))

        }
    }
}
