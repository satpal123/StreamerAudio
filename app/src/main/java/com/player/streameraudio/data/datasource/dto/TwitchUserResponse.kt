package com.player.streameraudio.data.datasource.dto

import com.google.gson.annotations.SerializedName

data class TwitchUserResponse(
    @SerializedName("data") val users: List<TwitchUser>
)