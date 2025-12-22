package com.player.streameraudio.data.datasource.dto

import com.google.gson.annotations.SerializedName

data class TwitchStreamersResponse(
    @SerializedName("data") val streamers: List<TwitchStream> = emptyList(),
    @SerializedName("pagination") val pagination: TwitchPaginationDTO? = null
)