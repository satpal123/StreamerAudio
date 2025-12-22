package com.player.streameraudio.data.datasource.dto

import com.google.gson.annotations.SerializedName

data class BackendChannelsResponse(
    @SerializedName("channels") val channels: List<BackendChannelDTO> = emptyList(),
    @SerializedName("pagination") val pagination: TwitchPaginationDTO? = null
)