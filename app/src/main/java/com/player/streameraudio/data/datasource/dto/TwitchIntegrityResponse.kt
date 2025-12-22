package com.player.streameraudio.data.datasource.dto

data class TwitchIntegrityResponse(
    val token: String,
    val expiration: Long // Token usually lasts 24 hours
)
