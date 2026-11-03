package com.player.streameraudio.data.datasource.dto

import com.google.gson.annotations.SerializedName

data class TwitchStream(
    @SerializedName("id")
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

    @SerializedName("type")
    val type: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("viewer_count")
    val viewerCount: Long,

    @SerializedName("started_at")
    val startedAt: String,

    @SerializedName("language")
    val language: String,

    @SerializedName("thumbnail_url")
    val thumbnailUrl: String,

    @SerializedName("tag_ids")
    val tagIds: List<Any?>,

    @SerializedName("tags")
    val tags: List<String>,
)
