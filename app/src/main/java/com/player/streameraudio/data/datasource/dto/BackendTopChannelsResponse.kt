package com.player.streameraudio.data.datasource.dto

import com.google.gson.annotations.SerializedName

data class BackendTopChannelsResponse (
    @SerializedName("data") val data: List<BackendChannelDTO> = emptyList(),
    @SerializedName("pagination") val pagination: TwitchPaginationDTO? = null
)
