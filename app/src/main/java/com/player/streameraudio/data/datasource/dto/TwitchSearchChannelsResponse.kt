package com.player.streameraudio.data.datasource.dto

import com.google.gson.annotations.SerializedName

data class TwitchSearchChannelsResponse(
    @SerializedName("data") val data: List<TwitchSearchChannelDTO> = emptyList(),
    @SerializedName("pagination") val pagination: TwitchPaginationDTO? = null
)


