package com.player.streameraudio.data.datasource.dto

import com.google.gson.annotations.SerializedName

data class TwitchStream(
    val id: String,

    @SerializedName("user_id")
    val userId: String,

    @SerializedName("user_login")
    val userLogin: String,

    @SerializedName("user_name")
    val userName: String,

    @SerializedName("game_id")
    val gameId: String,

    @SerializedName("game_name")
    val gameName: String,

    val type: String,
    val title: String,

    @SerializedName("viewer_count")
    val viewerCount: Long,

    @SerializedName("started_at")
    val startedAt: String,

    val language: String,

    @SerializedName("thumbnail_url")
    val thumbnailUrl: String,

    @SerializedName("tag_ids")
    val tagIds: List<Any?>,

    val tags: List<String>,
)
