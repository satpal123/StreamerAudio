package com.player.streameraudio.data.datasource.dto

import com.google.gson.annotations.SerializedName

data class TwitchSearchChannelDTO(
    @SerializedName("id") val id: String,
    @SerializedName("broadcaster_login") val broadcasterLogin: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("is_live") val isLive: Boolean
)
