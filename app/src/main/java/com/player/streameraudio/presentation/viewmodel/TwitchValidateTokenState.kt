package com.player.streameraudio.presentation.viewmodel

import com.player.streameraudio.data.datasource.dto.TwitchValidationDTO

data class TwitchValidateTokenState(
    val isLoading : Boolean = false,
    val tokenResponse: TwitchValidationDTO = TwitchValidationDTO(
        clientId = "",
        login = "",
        scopes = emptyList(),
        userId = "",
        expiresIn = 0L
    ),
    val error : String = ""
)
