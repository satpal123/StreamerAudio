package com.player.streameraudio.data.datasource.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class TwitchDtoSerializationTest {
    private val gson = Gson()

    @Test
    fun `token validation fields survive release name obfuscation`() {
        val result = gson.fromJson(
            """{
                "client_id":"client",
                "login":"listener",
                "scopes":["user:read:follows"],
                "user_id":"42",
                "expires_in":3600
            }""".trimIndent(),
            TwitchValidationDTO::class.java
        )

        assertEquals("client", result.clientId)
        assertEquals("listener", result.login)
        assertEquals(listOf("user:read:follows"), result.scopes)
        assertEquals("42", result.userId)
        assertEquals(3600L, result.expiresIn)
    }
}
