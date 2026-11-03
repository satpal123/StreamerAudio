package com.player.streameraudio.data.repository

import com.player.streameraudio.data.api.TwitchOauthApiService
import com.player.streameraudio.data.datasource.dto.TwitchValidationDTO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TwitchValidateRepositoryImplTest {
    private val api = mockk<TwitchOauthApiService>()
    private val repository = TwitchValidateRepositoryImpl(api)
    private val response = TwitchValidationDTO(
        clientId = "client",
        login = "listener",
        scopes = emptyList(),
        userId = "user",
        expiresIn = 3600
    )

    @Test
    fun `raw token receives one OAuth prefix`() = runTest {
        coEvery { api.validateAccessToken("OAuth valid-token") } returns response

        assertEquals(response, repository.validateAccessToken("valid-token"))

        coVerify(exactly = 1) { api.validateAccessToken("OAuth valid-token") }
    }

    @Test
    fun `already prefixed token is normalized without duplicate OAuth prefix`() = runTest {
        coEvery { api.validateAccessToken("OAuth valid-token") } returns response

        assertEquals(response, repository.validateAccessToken("OAuth valid-token"))

        coVerify(exactly = 1) { api.validateAccessToken("OAuth valid-token") }
    }
}
