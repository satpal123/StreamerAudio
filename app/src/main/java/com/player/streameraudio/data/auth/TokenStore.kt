package com.player.streameraudio.data.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthSession(
    val accessToken: String,
    val userId: String? = null,
    val userLogin: String? = null
)

interface TokenStore {
    fun read(): AuthSession?
    fun write(session: AuthSession)
    fun clear()
}
