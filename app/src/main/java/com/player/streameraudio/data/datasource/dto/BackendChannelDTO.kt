package com.player.streameraudio.data.datasource.dto

import com.google.gson.annotations.SerializedName

data class BackendChannelDTO(
    @SerializedName("id") val id: String,
    @SerializedName("user_login") val userLogin: String,
    @SerializedName("user_name") val userName: String,
    @SerializedName("title") val title: String,
    @SerializedName("viewer_count") val viewerCount: Long,
    @SerializedName("thumbnail_url") val thumbnailUrl: String,
    @SerializedName("started_at") val startedAt: String,
    @SerializedName("gameName") val gameName: String, //TODO: change SerializedName to game_name in backend api
)

