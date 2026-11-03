package com.player.streameraudio.data.datasource.dto

import com.google.gson.annotations.SerializedName

data class TwitchValidationDTO(
    @SerializedName("client_id")
    val clientId: String,

    @SerializedName("login")
    val login: String,

    @SerializedName("scopes")
    val scopes: List<String>,

    @SerializedName("user_id")
    val userId: String,

    @SerializedName("expires_in")
    val expiresIn: Long
)
