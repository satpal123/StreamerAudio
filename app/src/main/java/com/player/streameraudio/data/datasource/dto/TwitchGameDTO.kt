package com.player.streameraudio.data.datasource.dto

import com.google.gson.annotations.SerializedName

data class TwitchGameDTO(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("box_art_url") val boxArtUrl: String
)
