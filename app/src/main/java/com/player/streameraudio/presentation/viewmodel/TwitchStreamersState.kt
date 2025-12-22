package com.player.streameraudio.presentation.viewmodel

import com.player.streameraudio.data.datasource.dto.TwitchStream

data class TwitchStreamersState(
    val isLoading: Boolean = false,
    val streamersList: List<TwitchStream> = emptyList(),
    val error: String = ""
)
