package com.player.streameraudio.data.datasource.dto

import com.google.gson.annotations.SerializedName

data class TwitchPaginationDTO(
    @SerializedName("cursor") val cursor: String? = null
)