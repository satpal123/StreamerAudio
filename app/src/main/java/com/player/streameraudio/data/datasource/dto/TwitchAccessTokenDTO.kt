package com.player.streameraudio.data.datasource.dto

data class TwitchAccessTokenDTO(
    val accessToken: String,
    val scope: String,
    val tokenType: String
)