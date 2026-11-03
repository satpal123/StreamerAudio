package com.player.streameraudio.data.datasource.dto

import com.google.gson.annotations.SerializedName

data class TwitchIntegrityResponse(
    @SerializedName("token")
    val token: String,

    @SerializedName("expiration")
    val expiration: Long // Token usually lasts 24 hours
)
