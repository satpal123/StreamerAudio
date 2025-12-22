package com.player.streameraudio.domain.usecase

import com.player.streameraudio.data.datasource.dto.TwitchPaginationDTO
import com.player.streameraudio.data.datasource.dto.TwitchSearchChannelDTO
import com.player.streameraudio.data.datasource.dto.TwitchSearchChannelsResponse
import com.player.streameraudio.data.util.ResponseState
import com.player.streameraudio.domain.repository.TwitchRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Comprehensive unit tests for SearchTwitchChannelsUseCase
 * Tests channel search functionality including:
 * - Success scenarios with and without pagination
 * - Empty search results
 * - Error handling (HTTP errors, network errors)
 * - Flow emission sequence
 * - Pagination cursor handling
 */
class SearchTwitchChannelsUseCaseTest {

    private lateinit var repository: TwitchRepository
    private lateinit var useCase: SearchTwitchChannelsUseCase

    private val testAuthorization = "Bearer test_token"
    private val testClientId = "test_client_id"
    private val testQuery = "test_query"

    @Before
    fun setUp() {
        repository = mockk()
        useCase = SearchTwitchChannelsUseCase(repository)
    }

    // ========== Success Scenarios ==========

    @Test
    fun `invoke with search query emits Loading then Success with channels and cursor`() = runTest {
        // Given
        val channels = listOf(
            TwitchSearchChannelDTO(
                id = "channel1",
                broadcasterLogin = "user1",
                displayName = "User One",
                thumbnailUrl = "https://example.com/thumb1.jpg",
                title = "Live Stream 1",
                isLive = true
            ),
            TwitchSearchChannelDTO(
                id = "channel2",
                broadcasterLogin = "user2",
                displayName = "User Two",
                thumbnailUrl = "https://example.com/thumb2.jpg",
                title = "Live Stream 2",
                isLive = false
            )
        )
        val pagination = TwitchPaginationDTO(cursor = "next_cursor_123")
        val response = TwitchSearchChannelsResponse(data = channels, pagination = pagination)

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, testQuery, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, testQuery).toList()

        // Then
        assertEquals(2, result.size)

        // First emission: Loading
        assertTrue(result[0] is ResponseState.Loading)
        assertNull(result[0].data)

        // Second emission: Success
        assertTrue(result[1] is ResponseState.Success)
        val successData = result[1].data!!
        assertEquals(2, successData.first.size)
        assertEquals("next_cursor_123", successData.second)
        assertEquals("channel1", successData.first[0].id)
        assertEquals("User One", successData.first[0].displayName)
        assertTrue(successData.first[0].isLive)

        coVerify(exactly = 1) {
            repository.searchChannels(testAuthorization, testClientId, testQuery, null)
        }
    }

    @Test
    fun `invoke with pagination cursor passes it to repository`() = runTest {
        // Given
        val channels = listOf(
            TwitchSearchChannelDTO(
                id = "channel3",
                broadcasterLogin = "user3",
                displayName = "User Three",
                thumbnailUrl = null,
                title = null,
                isLive = true
            )
        )
        val pagination = TwitchPaginationDTO(cursor = "final_cursor")
        val response = TwitchSearchChannelsResponse(data = channels, pagination = pagination)
        val afterCursor = "previous_cursor_456"

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, testQuery, afterCursor)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, testQuery, afterCursor).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertEquals(1, successState.data.first.size)
        assertEquals("final_cursor", successState.data.second)

        coVerify(exactly = 1) {
            repository.searchChannels(testAuthorization, testClientId, testQuery, afterCursor)
        }
    }

    @Test
    fun `invoke returns channels without pagination cursor when pagination is null`() = runTest {
        // Given
        val channels = listOf(
            TwitchSearchChannelDTO(
                id = "channel1",
                broadcasterLogin = "user1",
                displayName = "User",
                thumbnailUrl = "thumb.jpg",
                title = "Stream",
                isLive = true
            )
        )
        val response = TwitchSearchChannelsResponse(data = channels, pagination = null)

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, testQuery, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, testQuery).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertEquals(1, successState.data.first.size)
        assertNull(successState.data.second)
    }

    @Test
    fun `invoke returns empty list when no channels found`() = runTest {
        // Given
        val response = TwitchSearchChannelsResponse(data = emptyList(), pagination = null)

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, testQuery, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, testQuery).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertTrue(successState.data.first.isEmpty())
        assertNull(successState.data.second)
    }

    @Test
    fun `invoke with different query strings returns correct results`() = runTest {
        // Given
        val query1 = "gaming"
        val query2 = "music"
        val channels1 = listOf(
            TwitchSearchChannelDTO("1", "gamer1", "Gamer", null, "Gaming Stream", true)
        )
        val channels2 = listOf(
            TwitchSearchChannelDTO("2", "musician1", "Musician", null, "Music Stream", true)
        )
        val response1 = TwitchSearchChannelsResponse(data = channels1, pagination = null)
        val response2 = TwitchSearchChannelsResponse(data = channels2, pagination = null)

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, query1, null)
        } returns response1
        coEvery {
            repository.searchChannels(testAuthorization, testClientId, query2, null)
        } returns response2

        // When
        val result1 = useCase(testAuthorization, testClientId, query1).toList()
        val result2 = useCase(testAuthorization, testClientId, query2).toList()

        // Then
        val success1 = result1.last() as ResponseState.Success
        val success2 = result2.last() as ResponseState.Success

        assertEquals("Gamer", success1.data.first[0].displayName)
        assertEquals("Musician", success2.data.first[0].displayName)
    }

    @Test
    fun `invoke with multiple pages of results`() = runTest {
        // Given
        val channels = listOf(
            TwitchSearchChannelDTO("1", "user1", "User 1", null, null, true),
            TwitchSearchChannelDTO("2", "user2", "User 2", null, null, false),
            TwitchSearchChannelDTO("3", "user3", "User 3", null, null, true),
            TwitchSearchChannelDTO("4", "user4", "User 4", null, null, true),
            TwitchSearchChannelDTO("5", "user5", "User 5", null, null, false)
        )
        val pagination = TwitchPaginationDTO(cursor = "page2_cursor")
        val response = TwitchSearchChannelsResponse(data = channels, pagination = pagination)

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, testQuery, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, testQuery).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertEquals(5, successState.data.first.size)
        assertEquals("page2_cursor", successState.data.second)
    }

    @Test
    fun `invoke with null and empty optional fields in channel data`() = runTest {
        // Given
        val channels = listOf(
            TwitchSearchChannelDTO(
                id = "channel1",
                broadcasterLogin = "user1",
                displayName = "User",
                thumbnailUrl = null,
                title = null,
                isLive = false
            )
        )
        val response = TwitchSearchChannelsResponse(data = channels, pagination = null)

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, testQuery, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, testQuery).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        val channel = successState.data.first[0]
        assertNull(channel.thumbnailUrl)
        assertNull(channel.title)
        assertFalse(channel.isLive)
    }

    // ========== Error Scenarios: HTTP Exceptions ==========

    @Test
    fun `invoke when HttpException occurs emits Loading then Error`() = runTest {
        // Given
        val httpException = HttpException(Response.error<Any>(404, mockk(relaxed = true)))

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, testQuery, null)
        } throws httpException

        // When
        val result = useCase(testAuthorization, testClientId, testQuery).toList()

        // Then
        assertEquals(2, result.size)

        // First emission: Loading
        assertTrue(result[0] is ResponseState.Loading)

        // Second emission: Error
        assertTrue(result[1] is ResponseState.Error)
        assertNotNull(result[1].message)
        assertNull(result[1].data)

        coVerify(exactly = 1) {
            repository.searchChannels(testAuthorization, testClientId, testQuery, null)
        }
    }

    @Test
    fun `invoke when 401 Unauthorized occurs`() = runTest {
        // Given
        val httpException = HttpException(Response.error<Any>(401, mockk(relaxed = true)))

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, testQuery, null)
        } throws httpException

        // When
        val result = useCase(testAuthorization, testClientId, testQuery).toList()

        // Then
        val errorState = result.last() as ResponseState.Error
        assertNotNull(errorState.message)
    }

    @Test
    fun `invoke when 500 Internal Server Error occurs`() = runTest {
        // Given
        val httpException = HttpException(Response.error<Any>(500, mockk(relaxed = true)))

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, testQuery, null)
        } throws httpException

        // When
        val result = useCase(testAuthorization, testClientId, testQuery).toList()

        // Then
        val errorState = result.last() as ResponseState.Error
        assertNotNull(errorState.message)
    }

    // ========== Error Scenarios: Network Errors ==========

    @Test
    fun `invoke when IOException occurs emits Loading then Error with Internet Error message`() = runTest {
        // Given
        val ioException = IOException("Network failure")

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, testQuery, null)
        } throws ioException

        // When
        val result = useCase(testAuthorization, testClientId, testQuery).toList()

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
        val ioException = IOException("Timeout")

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, testQuery, null)
        } throws ioException

        // When
        val result = useCase(testAuthorization, testClientId, testQuery).toList()

        // Then
        val errorState = result.last() as ResponseState.Error
        assertEquals("Internet Error", errorState.message)
    }

    // ========== Flow Behavior Tests ==========

    @Test
    fun `invoke always emits Loading state first`() = runTest {
        // Given
        val response = TwitchSearchChannelsResponse(data = emptyList(), pagination = null)

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, testQuery, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, testQuery).toList()

        // Then
        assertTrue(result.first() is ResponseState.Loading)
    }

    @Test
    fun `invoke emits exactly two states on success`() = runTest {
        // Given
        val response = TwitchSearchChannelsResponse(data = emptyList(), pagination = null)

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, testQuery, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, testQuery).toList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result[0] is ResponseState.Loading)
        assertTrue(result[1] is ResponseState.Success)
    }

    @Test
    fun `invoke emits exactly two states on error`() = runTest {
        // Given
        val ioException = IOException("Error")

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, testQuery, null)
        } throws ioException

        // When
        val result = useCase(testAuthorization, testClientId, testQuery).toList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result[0] is ResponseState.Loading)
        assertTrue(result[1] is ResponseState.Error)
    }

    // ========== Edge Cases ==========

    @Test
    fun `invoke with empty query string`() = runTest {
        // Given
        val emptyQuery = ""
        val response = TwitchSearchChannelsResponse(data = emptyList(), pagination = null)

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, emptyQuery, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, emptyQuery).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertTrue(successState.data.first.isEmpty())

        coVerify(exactly = 1) {
            repository.searchChannels(testAuthorization, testClientId, emptyQuery, null)
        }
    }

    @Test
    fun `invoke with special characters in query`() = runTest {
        // Given
        val specialQuery = "test@#$%^&*()"
        val response = TwitchSearchChannelsResponse(data = emptyList(), pagination = null)

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, specialQuery, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, specialQuery).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertNotNull(successState.data)
    }

    @Test
    fun `invoke with very long query string`() = runTest {
        // Given
        val longQuery = "a".repeat(500)
        val response = TwitchSearchChannelsResponse(data = emptyList(), pagination = null)

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, longQuery, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, longQuery).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertNotNull(successState.data)
    }

    @Test
    fun `invoke with unicode characters in query`() = runTest {
        // Given
        val unicodeQuery = "日本語テスト"
        val channels = listOf(
            TwitchSearchChannelDTO("1", "user", "ユーザー", null, null, true)
        )
        val response = TwitchSearchChannelsResponse(data = channels, pagination = null)

        coEvery {
            repository.searchChannels(testAuthorization, testClientId, unicodeQuery, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, unicodeQuery).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertEquals(1, successState.data.first.size)
        assertEquals("ユーザー", successState.data.first[0].displayName)
    }
}
