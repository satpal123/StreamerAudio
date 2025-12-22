package com.player.streameraudio.domain.usecase

import com.player.streameraudio.data.datasource.dto.TwitchValidationDTO
import com.player.streameraudio.data.util.ResponseState
import com.player.streameraudio.domain.repository.TwitchValidateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Comprehensive unit tests for GetValidTokenUseCase
 * Tests token validation flow including:
 * - Success scenarios
 * - Error handling (401, network errors, unexpected errors)
 * - Flow emission sequence (Loading -> Success/Error)
 * - Repository interaction verification
 */
class GetValidTokenUseCaseTest {

    private lateinit var repository: TwitchValidateRepository
    private lateinit var useCase: GetValidTokenUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetValidTokenUseCase(repository)
    }

    // ========== Success Scenarios ==========

    @Test
    fun `invoke with valid token emits Loading then Success`() = runTest {
        // Given
        val authorization = "Bearer valid_token_123"
        val expectedValidation = TwitchValidationDTO(
            clientId = "client_id_123",
            login = "test_user",
            scopes = listOf("user:read:email", "user:read:follows"),
            userId = "user_123",
            expiresIn = 3600L
        )

        coEvery { repository.validateAccessToken(authorization) } returns expectedValidation

        // When
        val result = useCase(authorization).toList()

        // Then
        assertEquals(2, result.size)

        // First emission: Loading
        assertTrue(result[0] is ResponseState.Loading)
        assertNull(result[0].data)

        // Second emission: Success
        assertTrue(result[1] is ResponseState.Success)
        val successData = (result[1] as ResponseState.Success).data
        assertEquals(expectedValidation, successData)
        assertEquals("client_id_123", successData.clientId)
        assertNotNull(successData)
        assertEquals("test_user", successData.login)
        assertNotNull(successData)
        assertEquals(2, successData.scopes.size)
        assertNotNull(successData)
        assertEquals("user_123", successData.userId)
        assertNotNull(successData)
        assertEquals(3600L, successData.expiresIn)

        // Verify repository was called
        coVerify(exactly = 1) { repository.validateAccessToken(authorization) }
    }

    @Test
    fun `invoke with different token returns correct validation data`() = runTest {
        // Given
        val authorization = "Bearer another_token_456"
        val expectedValidation = TwitchValidationDTO(
            clientId = "different_client",
            login = "another_user",
            scopes = listOf("user:read:email"),
            userId = "user_456",
            expiresIn = 7200L
        )

        coEvery { repository.validateAccessToken(authorization) } returns expectedValidation

        // When
        val result = useCase(authorization).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertEquals("different_client", successState.data.clientId)
        assertEquals("another_user", successState.data.login)
        assertEquals(1, successState.data.scopes.size)
        assertEquals("user_456", successState.data.userId)
        assertEquals(7200L, successState.data.expiresIn)
    }

    @Test
    fun `invoke with token having multiple scopes`() = runTest {
        // Given
        val authorization = "Bearer token_with_scopes"
        val expectedValidation = TwitchValidationDTO(
            clientId = "client_id",
            login = "user",
            scopes = listOf(
                "user:read:email",
                "user:read:follows",
                "user:edit:broadcast",
                "channel:read:subscriptions"
            ),
            userId = "user_id",
            expiresIn = 5000L
        )

        coEvery { repository.validateAccessToken(authorization) } returns expectedValidation

        // When
        val result = useCase(authorization).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertEquals(4, successState.data.scopes.size)
        assertTrue(successState.data.scopes.contains("user:read:email"))
        assertTrue(successState.data.scopes.contains("channel:read:subscriptions"))
    }

    @Test
    fun `invoke with token having no scopes`() = runTest {
        // Given
        val authorization = "Bearer token_no_scopes"
        val expectedValidation = TwitchValidationDTO(
            clientId = "client_id",
            login = "user",
            scopes = emptyList(),
            userId = "user_id",
            expiresIn = 3600L
        )

        coEvery { repository.validateAccessToken(authorization) } returns expectedValidation

        // When
        val result = useCase(authorization).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertTrue(successState.data.scopes.isEmpty())
    }

    // ========== Error Scenarios: 401 Unauthorized ==========

    @Test
    fun `invoke with invalid token emits Loading then Error with invalid access token message`() = runTest {
        // Given
        val authorization = "Bearer invalid_token"
        val exception = Exception("HTTP 401 Unauthorized")

        coEvery { repository.validateAccessToken(authorization) } throws exception

        // When
        val result = useCase(authorization).toList()

        // Then
        assertEquals(2, result.size)

        // First emission: Loading
        assertTrue(result[0] is ResponseState.Loading)

        // Second emission: Error
        assertTrue(result[1] is ResponseState.Error)
        assertEquals("invalid access token", result[1].message)
        assertNull(result[1].data)

        coVerify(exactly = 1) { repository.validateAccessToken(authorization) }
    }

    @Test
    fun `invoke when repository throws 401 error`() = runTest {
        // Given
        val authorization = "Bearer expired_token"
        val exception = Exception("401 - Token expired")

        coEvery { repository.validateAccessToken(authorization) } throws exception

        // When
        val result = useCase(authorization).toList()

        // Then
        val errorState = result.last() as ResponseState.Error
        assertEquals("invalid access token", errorState.message)
    }

    @Test
    fun `invoke with 401 in exception message case insensitive`() = runTest {
        // Given
        val authorization = "Bearer token"
        val exception = Exception("Error: 401 Forbidden")

        coEvery { repository.validateAccessToken(authorization) } throws exception

        // When
        val result = useCase(authorization).toList()

        // Then
        val errorState = result.last() as ResponseState.Error
        assertEquals("invalid access token", errorState.message)
    }

    // ========== Error Scenarios: Network Errors ==========

    @Test
    fun `invoke when IOException occurs emits Loading then Error with Internet Error message`() = runTest {
        // Given
        val authorization = "Bearer token"
        val exception = IOException("No network connection")

        coEvery { repository.validateAccessToken(authorization) } throws exception

        // When
        val result = useCase(authorization).toList()

        // Then
        assertEquals(2, result.size)

        // First emission: Loading
        assertTrue(result[0] is ResponseState.Loading)

        // Second emission: Error
        assertTrue(result[1] is ResponseState.Error)
        assertEquals("Internet Error", result[1].message)
        assertNull(result[1].data)
    }

    @Test
    fun `invoke when network timeout occurs`() = runTest {
        // Given
        val authorization = "Bearer token"
        val exception = IOException("Connect timeout")

        coEvery { repository.validateAccessToken(authorization) } throws exception

        // When
        val result = useCase(authorization).toList()

        // Then
        val errorState = result.last() as ResponseState.Error
        assertEquals("Internet Error", errorState.message)
    }

    @Test
    fun `invoke when no internet connection`() = runTest {
        // Given
        val authorization = "Bearer token"
        val exception = IOException("Unable to resolve host")

        coEvery { repository.validateAccessToken(authorization) } throws exception

        // When
        val result = useCase(authorization).toList()

        // Then
        val errorState = result.last() as ResponseState.Error
        assertEquals("Internet Error", errorState.message)
    }

    // ========== Error Scenarios: Unknown Errors ==========

    @Test
    fun `invoke when unknown exception occurs emits Loading then Error with unknown error message`() = runTest {
        // Given
        val authorization = "Bearer token"
        val exception = Exception("Something went wrong")

        coEvery { repository.validateAccessToken(authorization) } throws exception

        // When
        val result = useCase(authorization).toList()

        // Then
        assertEquals(2, result.size)

        // First emission: Loading
        assertTrue(result[0] is ResponseState.Loading)

        // Second emission: Error
        assertTrue(result[1] is ResponseState.Error)
        assertEquals("Unknown error occurred: Something went wrong", result[1].message)
        assertNull(result[1].data)
    }

    @Test
    fun `invoke when null pointer exception occurs`() = runTest {
        // Given
        val authorization = "Bearer token"
        val exception = NullPointerException("Null reference")

        coEvery { repository.validateAccessToken(authorization) } throws exception

        // When
        val result = useCase(authorization).toList()

        // Then
        val errorState = result.last() as ResponseState.Error
        assertTrue(errorState.message!!.contains("Unknown error occurred"))
        assertTrue(errorState.message!!.contains("Null reference"))
    }

    @Test
    fun `invoke when runtime exception occurs`() = runTest {
        // Given
        val authorization = "Bearer token"
        val exception = RuntimeException("Runtime error")

        coEvery { repository.validateAccessToken(authorization) } throws exception

        // When
        val result = useCase(authorization).toList()

        // Then
        val errorState = result.last() as ResponseState.Error
        assertEquals("Unknown error occurred: Runtime error", errorState.message)
    }

    @Test
    fun `invoke when exception has no message`() = runTest {
        // Given
        val authorization = "Bearer token"
        val exception = Exception()

        coEvery { repository.validateAccessToken(authorization) } throws exception

        // When
        val result = useCase(authorization).toList()

        // Then
        val errorState = result.last() as ResponseState.Error
        assertTrue(errorState.message!!.contains("Unknown error occurred"))
    }

    // ========== Flow Behavior Tests ==========

    @Test
    fun `invoke always emits Loading state first`() = runTest {
        // Given
        val authorization = "Bearer token"
        val expectedValidation = TwitchValidationDTO(
            clientId = "client",
            login = "user",
            scopes = listOf(),
            userId = "id",
            expiresIn = 3600L
        )

        coEvery { repository.validateAccessToken(authorization) } returns expectedValidation

        // When
        val result = useCase(authorization).toList()

        // Then
        assertTrue(result.first() is ResponseState.Loading)
    }

    @Test
    fun `invoke emits exactly two states on success`() = runTest {
        // Given
        val authorization = "Bearer token"
        val expectedValidation = TwitchValidationDTO(
            clientId = "client",
            login = "user",
            scopes = listOf(),
            userId = "id",
            expiresIn = 3600L
        )

        coEvery { repository.validateAccessToken(authorization) } returns expectedValidation

        // When
        val result = useCase(authorization).toList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result[0] is ResponseState.Loading)
        assertTrue(result[1] is ResponseState.Success)
    }

    @Test
    fun `invoke emits exactly two states on error`() = runTest {
        // Given
        val authorization = "Bearer token"
        val exception = Exception("Error")

        coEvery { repository.validateAccessToken(authorization) } throws exception

        // When
        val result = useCase(authorization).toList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result[0] is ResponseState.Loading)
        assertTrue(result[1] is ResponseState.Error)
    }

    // ========== Multiple Invocations ==========

    @Test
    fun `multiple invocations with different tokens produce independent results`() = runTest {
        // Given
        val token1 = "Bearer token1"
        val token2 = "Bearer token2"
        val validation1 = TwitchValidationDTO("client1", "user1", listOf(), "id1", 3600L)
        val validation2 = TwitchValidationDTO("client2", "user2", listOf(), "id2", 7200L)

        coEvery { repository.validateAccessToken(token1) } returns validation1
        coEvery { repository.validateAccessToken(token2) } returns validation2

        // When
        val result1 = useCase(token1).toList()
        val result2 = useCase(token2).toList()

        // Then
        val success1 = result1.last() as ResponseState.Success
        val success2 = result2.last() as ResponseState.Success

        assertEquals("user1", success1.data.login)
        assertEquals("user2", success2.data.login)
        assertEquals(3600L, success1.data.expiresIn)
        assertEquals(7200L, success2.data.expiresIn)

        coVerify(exactly = 1) { repository.validateAccessToken(token1) }
        coVerify(exactly = 1) { repository.validateAccessToken(token2) }
    }

    // ========== Edge Cases ==========

    @Test
    fun `invoke with empty authorization string`() = runTest {
        // Given
        val authorization = ""
        val expectedValidation = TwitchValidationDTO(
            clientId = "client",
            login = "user",
            scopes = listOf(),
            userId = "id",
            expiresIn = 3600L
        )

        coEvery { repository.validateAccessToken(authorization) } returns expectedValidation

        // When
        val result = useCase(authorization).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertNotNull(successState.data)
        coVerify(exactly = 1) { repository.validateAccessToken(authorization) }
    }

    @Test
    fun `invoke with very long authorization token`() = runTest {
        // Given
        val authorization = "Bearer " + "a".repeat(1000)
        val expectedValidation = TwitchValidationDTO(
            clientId = "client",
            login = "user",
            scopes = listOf(),
            userId = "id",
            expiresIn = 3600L
        )

        coEvery { repository.validateAccessToken(authorization) } returns expectedValidation

        // When
        val result = useCase(authorization).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertNotNull(successState.data)
    }
}
