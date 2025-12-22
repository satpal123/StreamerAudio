package com.player.streameraudio.domain.usecase

import com.player.streameraudio.data.datasource.dto.TwitchPaginationDTO
import com.player.streameraudio.data.datasource.dto.TwitchStream
import com.player.streameraudio.data.datasource.dto.TwitchStreamersResponse
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
 * Comprehensive unit tests for GetStreamsByGameUseCase
 * Tests fetching streams by game ID including:
 * - Success scenarios with and without pagination
 * - Empty results
 * - Error handling (HTTP errors, network errors)
 * - Flow emission sequence
 * - Pagination cursor handling
 * - Stream data validation
 */
class GetStreamsByGameUseCaseTest {

    private lateinit var repository: TwitchRepository
    private lateinit var useCase: GetStreamsByGameUseCase

    private val testAuthorization = "Bearer test_token"
    private val testClientId = "test_client_id"
    private val testGameId = "game_123"

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetStreamsByGameUseCase(repository)
    }

    // ========== Success Scenarios ==========

    @Test
    fun `invoke with game ID emits Loading then Success with streams and cursor`() = runTest {
        // Given
        val streams = listOf(
            TwitchStream(
                id = "stream1",
                userId = "user1",
                userLogin = "streamer1",
                userName = "Streamer One",
                gameId = "game_123",
                gameName = "Test Game",
                type = "live",
                title = "Playing Test Game",
                viewerCount = 1000L,
                startedAt = "2023-01-01T12:00:00Z",
                language = "en",
                thumbnailUrl = "https://example.com/thumb1.jpg",
                tagIds = listOf(),
                tags = listOf("English", "Gaming")
            ),
            TwitchStream(
                id = "stream2",
                userId = "user2",
                userLogin = "streamer2",
                userName = "Streamer Two",
                gameId = "game_123",
                gameName = "Test Game",
                type = "live",
                title = "Another Stream",
                viewerCount = 500L,
                startedAt = "2023-01-01T13:00:00Z",
                language = "es",
                thumbnailUrl = "https://example.com/thumb2.jpg",
                tagIds = listOf(),
                tags = listOf("Spanish")
            )
        )
        val pagination = TwitchPaginationDTO(cursor = "next_cursor_abc")
        val response = TwitchStreamersResponse(streamers = streams, pagination = pagination)

        coEvery {
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, testGameId).toList()

        // Then
        assertEquals(2, result.size)

        // First emission: Loading
        assertTrue(result[0] is ResponseState.Loading)
        assertNull(result[0].data)

        // Second emission: Success
        assertTrue(result[1] is ResponseState.Success)
        val successData = result[1].data!!
        assertEquals(2, successData.first.size)
        assertEquals("next_cursor_abc", successData.second)
        assertEquals("stream1", successData.first[0].id)
        assertEquals("Streamer One", successData.first[0].userName)
        assertEquals(1000L, successData.first[0].viewerCount)

        coVerify(exactly = 1) {
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, null)
        }
    }

    @Test
    fun `invoke with pagination cursor passes it to repository`() = runTest {
        // Given
        val streams = listOf(
            TwitchStream(
                id = "stream3",
                userId = "user3",
                userLogin = "streamer3",
                userName = "Streamer Three",
                gameId = "game_123",
                gameName = "Test Game",
                type = "live",
                title = "Third Stream",
                viewerCount = 250L,
                startedAt = "2023-01-01T14:00:00Z",
                language = "en",
                thumbnailUrl = "thumb.jpg",
                tagIds = listOf(),
                tags = listOf()
            )
        )
        val pagination = TwitchPaginationDTO(cursor = "final_cursor")
        val response = TwitchStreamersResponse(streamers = streams, pagination = pagination)
        val afterCursor = "previous_cursor_xyz"

        coEvery {
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, afterCursor)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, testGameId, afterCursor).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertEquals(1, successState.data.first.size)
        assertEquals("final_cursor", successState.data.second)
        assertEquals("stream3", successState.data.first[0].id)

        coVerify(exactly = 1) {
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, afterCursor)
        }
    }

    @Test
    fun `invoke returns streams without pagination cursor when pagination is null`() = runTest {
        // Given
        val streams = listOf(
            TwitchStream(
                id = "stream1",
                userId = "user1",
                userLogin = "streamer",
                userName = "Streamer",
                gameId = "game_123",
                gameName = "Game",
                type = "live",
                title = "Stream",
                viewerCount = 100L,
                startedAt = "2023-01-01T12:00:00Z",
                language = "en",
                thumbnailUrl = "thumb.jpg",
                tagIds = listOf(),
                tags = listOf()
            )
        )
        val response = TwitchStreamersResponse(streamers = streams, pagination = null)

        coEvery {
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, testGameId).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertEquals(1, successState.data.first.size)
        assertNull(successState.data.second)
    }

    @Test
    fun `invoke returns empty list when no streams found for game`() = runTest {
        // Given
        val response = TwitchStreamersResponse(streamers = emptyList(), pagination = null)

        coEvery {
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, testGameId).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertTrue(successState.data.first.isEmpty())
        assertNull(successState.data.second)
    }

    @Test
    fun `invoke with different game IDs returns correct results`() = runTest {
        // Given
        val gameId1 = "game_1"
        val gameId2 = "game_2"
        val streams1 = listOf(
            TwitchStream(
                "s1", "u1", "login1", "User1", gameId1, "Game One",
                "live", "Stream 1", 100L, "2023-01-01T12:00:00Z", "en",
                "thumb.jpg", listOf(), listOf()
            )
        )
        val streams2 = listOf(
            TwitchStream(
                "s2", "u2", "login2", "User2", gameId2, "Game Two",
                "live", "Stream 2", 200L, "2023-01-01T13:00:00Z", "es",
                "thumb.jpg", listOf(), listOf()
            )
        )
        val response1 = TwitchStreamersResponse(streamers = streams1, pagination = null)
        val response2 = TwitchStreamersResponse(streamers = streams2, pagination = null)

        coEvery {
            repository.getStreamsByGame(testAuthorization, testClientId, gameId1, null)
        } returns response1
        coEvery {
            repository.getStreamsByGame(testAuthorization, testClientId, gameId2, null)
        } returns response2

        // When
        val result1 = useCase(testAuthorization, testClientId, gameId1).toList()
        val result2 = useCase(testAuthorization, testClientId, gameId2).toList()

        // Then
        val success1 = result1.last() as ResponseState.Success
        val success2 = result2.last() as ResponseState.Success

        assertEquals("Game One", success1.data.first[0].gameName)
        assertEquals("Game Two", success2.data.first[0].gameName)
        assertEquals(100L, success1.data.first[0].viewerCount)
        assertEquals(200L, success2.data.first[0].viewerCount)
    }

    @Test
    fun `invoke with high viewer count streams`() = runTest {
        // Given
        val streams = listOf(
            TwitchStream(
                "s1", "u1", "login", "Popular Streamer", testGameId, "Game",
                "live", "Popular Stream", 50000L, "2023-01-01T12:00:00Z", "en",
                "thumb.jpg", listOf(), listOf("Popular", "Featured")
            )
        )
        val response = TwitchStreamersResponse(streamers = streams, pagination = null)

        coEvery {
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, testGameId).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertEquals(50000L, successState.data.first[0].viewerCount)
    }

    @Test
    fun `invoke with multiple streams sorted by viewer count`() = runTest {
        // Given
        val streams = listOf(
            TwitchStream(
                "s1", "u1", "login1", "User1", testGameId, "Game",
                "live", "Stream 1", 5000L, "2023-01-01T12:00:00Z", "en",
                "thumb.jpg", listOf(), listOf()
            ),
            TwitchStream(
                "s2", "u2", "login2", "User2", testGameId, "Game",
                "live", "Stream 2", 3000L, "2023-01-01T12:00:00Z", "en",
                "thumb.jpg", listOf(), listOf()
            ),
            TwitchStream(
                "s3", "u3", "login3", "User3", testGameId, "Game",
                "live", "Stream 3", 1000L, "2023-01-01T12:00:00Z", "en",
                "thumb.jpg", listOf(), listOf()
            )
        )
        val response = TwitchStreamersResponse(streamers = streams, pagination = null)

        coEvery {
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, testGameId).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertEquals(3, successState.data.first.size)
        assertEquals(5000L, successState.data.first[0].viewerCount)
        assertEquals(3000L, successState.data.first[1].viewerCount)
        assertEquals(1000L, successState.data.first[2].viewerCount)
    }

    @Test
    fun `invoke with streams in different languages`() = runTest {
        // Given
        val streams = listOf(
            TwitchStream(
                "s1", "u1", "login1", "User1", testGameId, "Game",
                "live", "English Stream", 100L, "2023-01-01T12:00:00Z", "en",
                "thumb.jpg", listOf(), listOf()
            ),
            TwitchStream(
                "s2", "u2", "login2", "User2", testGameId, "Game",
                "live", "Spanish Stream", 200L, "2023-01-01T12:00:00Z", "es",
                "thumb.jpg", listOf(), listOf()
            ),
            TwitchStream(
                "s3", "u3", "login3", "User3", testGameId, "Game",
                "live", "Japanese Stream", 150L, "2023-01-01T12:00:00Z", "ja",
                "thumb.jpg", listOf(), listOf()
            )
        )
        val response = TwitchStreamersResponse(streamers = streams, pagination = null)

        coEvery {
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, testGameId).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertEquals(3, successState.data.first.size)
        assertEquals("en", successState.data.first[0].language)
        assertEquals("es", successState.data.first[1].language)
        assertEquals("ja", successState.data.first[2].language)
    }

    // ========== Error Scenarios: HTTP Exceptions ==========

    @Test
    fun `invoke when HttpException occurs emits Loading then Error`() = runTest {
        // Given
        val httpException = HttpException(Response.error<Any>(404, mockk(relaxed = true)))

        coEvery {
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, null)
        } throws httpException

        // When
        val result = useCase(testAuthorization, testClientId, testGameId).toList()

        // Then
        assertEquals(2, result.size)

        // First emission: Loading
        assertTrue(result[0] is ResponseState.Loading)

        // Second emission: Error
        assertTrue(result[1] is ResponseState.Error)
        assertNotNull(result[1].message)
        assertNull(result[1].data)

        coVerify(exactly = 1) {
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, null)
        }
    }

    @Test
    fun `invoke when 401 Unauthorized occurs`() = runTest {
        // Given
        val httpException = HttpException(Response.error<Any>(401, mockk(relaxed = true)))

        coEvery {
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, null)
        } throws httpException

        // When
        val result = useCase(testAuthorization, testClientId, testGameId).toList()

        // Then
        val errorState = result.last() as ResponseState.Error
        assertNotNull(errorState.message)
    }

    @Test
    fun `invoke when 500 Internal Server Error occurs`() = runTest {
        // Given
        val httpException = HttpException(Response.error<Any>(500, mockk(relaxed = true)))

        coEvery {
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, null)
        } throws httpException

        // When
        val result = useCase(testAuthorization, testClientId, testGameId).toList()

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
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, null)
        } throws ioException

        // When
        val result = useCase(testAuthorization, testClientId, testGameId).toList()

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
        val ioException = IOException("Connect timeout")

        coEvery {
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, null)
        } throws ioException

        // When
        val result = useCase(testAuthorization, testClientId, testGameId).toList()

        // Then
        val errorState = result.last() as ResponseState.Error
        assertEquals("Internet Error", errorState.message)
    }

    // ========== Flow Behavior Tests ==========

    @Test
    fun `invoke always emits Loading state first`() = runTest {
        // Given
        val response = TwitchStreamersResponse(streamers = emptyList(), pagination = null)

        coEvery {
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, testGameId).toList()

        // Then
        assertTrue(result.first() is ResponseState.Loading)
    }

    @Test
    fun `invoke emits exactly two states on success`() = runTest {
        // Given
        val response = TwitchStreamersResponse(streamers = emptyList(), pagination = null)

        coEvery {
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, testGameId).toList()

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
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, null)
        } throws ioException

        // When
        val result = useCase(testAuthorization, testClientId, testGameId).toList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result[0] is ResponseState.Loading)
        assertTrue(result[1] is ResponseState.Error)
    }

    // ========== Edge Cases ==========

    @Test
    fun `invoke with empty game ID`() = runTest {
        // Given
        val emptyGameId = ""
        val response = TwitchStreamersResponse(streamers = emptyList(), pagination = null)

        coEvery {
            repository.getStreamsByGame(testAuthorization, testClientId, emptyGameId, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, emptyGameId).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertTrue(successState.data.first.isEmpty())

        coVerify(exactly = 1) {
            repository.getStreamsByGame(testAuthorization, testClientId, emptyGameId, null)
        }
    }

    @Test
    fun `invoke with numeric game ID`() = runTest {
        // Given
        val numericGameId = "123456789"
        val streams = listOf(
            TwitchStream(
                "s1", "u1", "login", "User", numericGameId, "Game",
                "live", "Stream", 100L, "2023-01-01T12:00:00Z", "en",
                "thumb.jpg", listOf(), listOf()
            )
        )
        val response = TwitchStreamersResponse(streamers = streams, pagination = null)

        coEvery {
            repository.getStreamsByGame(testAuthorization, testClientId, numericGameId, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, numericGameId).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertEquals(numericGameId, successState.data.first[0].gameId)
    }

    @Test
    fun `invoke with streams having zero viewers`() = runTest {
        // Given
        val streams = listOf(
            TwitchStream(
                "s1", "u1", "login", "New Streamer", testGameId, "Game",
                "live", "Just Started", 0L, "2023-01-01T12:00:00Z", "en",
                "thumb.jpg", listOf(), listOf()
            )
        )
        val response = TwitchStreamersResponse(streamers = streams, pagination = null)

        coEvery {
            repository.getStreamsByGame(testAuthorization, testClientId, testGameId, null)
        } returns response

        // When
        val result = useCase(testAuthorization, testClientId, testGameId).toList()

        // Then
        val successState = result.last() as ResponseState.Success
        assertEquals(0L, successState.data.first[0].viewerCount)
    }
}
