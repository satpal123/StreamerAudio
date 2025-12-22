package com.player.streameraudio.presentation.viewmodel

import com.player.streameraudio.data.datasource.dto.TwitchUser

data class TwitchUserState(
    val isLoading : Boolean = false,
    val userList : List<TwitchUser> = emptyList(),
    val error : String = ""
)
