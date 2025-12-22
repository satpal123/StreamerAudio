package com.player.streameraudio.data.datasource.dto

import com.google.gson.annotations.SerializedName

data class TwitchTopGamesResponse(
    @SerializedName("data") val data: List<TwitchGameDTO> = emptyList(),
    @SerializedName("pagination") val pagination: TwitchPaginationDTO? = null
)