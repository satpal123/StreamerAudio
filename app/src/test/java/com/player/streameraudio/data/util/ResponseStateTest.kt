package com.player.streameraudio.data.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive unit tests for ResponseState sealed class
 * Tests the three states: Loading, Success, and Error
 * Validates data and message handling across all states
 */
class ResponseStateTest {

    // ========== Loading State Tests ==========

    @Test
    fun `Loading state with null data`() {
        val state = ResponseState.Loading<String>()

        assertNull(state.data)
        assertNull(state.message)
        assertTrue(state is ResponseState.Loading<*>)
    }

    @Test
    fun `Loading state with data`() {
        val testData = "Loading data"
        val state = ResponseState.Loading(testData)

        assertEquals(testData, state.data)
        assertNull(state.message)
        assertTrue(state is ResponseState.Loading<*>)
    }

    @Test
    fun `Loading state with Int data`() {
        val testData = 42
        val state = ResponseState.Loading(testData)

        assertEquals(testData, state.data)
        assertNull(state.message)
    }

    @Test
    fun `Loading state with List data`() {
        val testData = listOf("item1", "item2", "item3")
        val state = ResponseState.Loading(testData)

        assertEquals(testData, state.data)
        assertEquals(3, state.data?.size)
    }

    @Test
    fun `Loading state with custom object`() {
        data class User(val id: Int, val name: String)
        val user = User(1, "John")
        val state = ResponseState.Loading(user)

        assertEquals(user, state.data)
        assertEquals(1, state.data?.id)
        assertEquals("John", state.data?.name)
    }

    // ========== Success State Tests ==========

    @Test
    fun `Success state with String data`() {
        val testData = "Success data"
        val state = ResponseState.Success(testData)

        assertEquals(testData, state.data)
        assertNull(state.message)
        assertTrue(state is ResponseState.Success<*>)
    }

    @Test
    fun `Success state with Int data`() {
        val testData = 100
        val state = ResponseState.Success(testData)

        assertEquals(testData, state.data)
        assertNull(state.message)
    }

    @Test
    fun `Success state with Boolean data`() {
        val state = ResponseState.Success(true)

        assertEquals(true, state.data)
        assertNull(state.message)
    }

    @Test
    fun `Success state with List data`() {
        val testData = listOf(1, 2, 3, 4, 5)
        val state = ResponseState.Success(testData)

        assertEquals(testData, state.data)
        assertNotNull(state.data)
        assertEquals(5, state.data!!.size)
        assertEquals(1, state.data!![0])
    }

    @Test
    fun `Success state with Map data`() {
        val testData = mapOf("key1" to "value1", "key2" to "value2")
        val state = ResponseState.Success(testData)

        assertEquals(testData, state.data)
        assertNotNull(state.data)
        assertEquals("value1", state.data!!["key1"])
        assertEquals(2, state.data!!.size)
    }

    @Test
    fun `Success state with custom data class`() {
        data class ApiResponse(val code: Int, val message: String, val data: List<String>)
        val response = ApiResponse(200, "OK", listOf("a", "b", "c"))
        val state = ResponseState.Success(response)

        assertEquals(response, state.data)
        assertNotNull(state.data)
        assertEquals(200, state.data!!.code)
        assertEquals("OK", state.data!!.message)
        assertEquals(3, state.data!!.data.size)
    }

    @Test
    fun `Success state with nullable type but non-null value`() {
        val testData: String? = "Non-null value"
        val state = ResponseState.Success(testData)

        assertEquals(testData, state.data)
        assertNotNull(state.data)
    }

    @Test
    fun `Success state with null value for nullable type`() {
        val testData: String? = null
        val state = ResponseState.Success(testData)

        assertNull(state.data)
        assertTrue(state is ResponseState.Success<*>)
    }

    @Test
    fun `Success state with empty string`() {
        val state = ResponseState.Success("")

        assertEquals("", state.data)
        assertNotNull(state.data)
    }

    @Test
    fun `Success state with empty list`() {
        val state = ResponseState.Success(emptyList<String>())

        assertNotNull(state.data)
        assertTrue(state.data!!.isEmpty())
    }

    // ========== Error State Tests ==========

    @Test
    fun `Error state with message and null data`() {
        val errorMessage = "An error occurred"
        val state = ResponseState.Error<String>(errorMessage)

        assertEquals(errorMessage, state.message)
        assertNull(state.data)
        assertTrue(state is ResponseState.Error<*>)
    }

    @Test
    fun `Error state with message and data`() {
        val errorMessage = "Partial failure"
        val testData = "Partial data"
        val state = ResponseState.Error(errorMessage, testData)

        assertEquals(errorMessage, state.message)
        assertEquals(testData, state.data)
    }

    @Test
    fun `Error state with network error message`() {
        val errorMessage = "Internet Error"
        val state = ResponseState.Error<Any>(errorMessage)

        assertEquals(errorMessage, state.message)
        assertNull(state.data)
    }

    @Test
    fun `Error state with HTTP error message`() {
        val errorMessage = "HTTP 404 Not Found"
        val state = ResponseState.Error<Any>(errorMessage)

        assertEquals(errorMessage, state.message)
    }

    @Test
    fun `Error state with validation error and partial data`() {
        data class FormData(val username: String, val email: String)
        val errorMessage = "Invalid email format"
        val partialData = FormData("user123", "invalid-email")
        val state = ResponseState.Error(errorMessage, partialData)

        assertEquals(errorMessage, state.message)
        assertEquals(partialData, state.data)
        assertEquals("user123", state.data?.username)
    }

    @Test
    fun `Error state with empty error message`() {
        val state = ResponseState.Error<String>("")

        assertEquals("", state.message)
        assertNotNull(state.message)
        assertNull(state.data)
    }

    @Test
    fun `Error state with Long error message`() {
        val errorMessage = "This is a very long error message that contains detailed information " +
                "about what went wrong during the operation. It includes stack traces, " +
                "error codes, and suggestions for how to fix the issue."
        val state = ResponseState.Error<String>(errorMessage)

        assertEquals(errorMessage, state.message)
        assertTrue(state.message!!.length > 100)
    }

    @Test
    fun `Error state with special characters in message`() {
        val errorMessage = "Error: Invalid input! @#\$%^&*()"
        val state = ResponseState.Error<String>(errorMessage)

        assertEquals(errorMessage, state.message)
    }

    @Test
    fun `Error state with multiline error message`() {
        val errorMessage = "Error on line 1\nError on line 2\nError on line 3"
        val state = ResponseState.Error<String>(errorMessage)

        assertEquals(errorMessage, state.message)
        assertTrue(state.message!!.contains("\n"))
    }

    // ========== Type Safety and Polymorphism Tests ==========

    @Test
    fun `Different states can be assigned to same variable`() {
        var state: ResponseState<String> = ResponseState.Loading()
        assertTrue(state is ResponseState.Loading<*>)

        state = ResponseState.Success("Success")
        assertTrue(state is ResponseState.Success<*>)
        assertEquals("Success", state.data)

        state = ResponseState.Error("Error")
        assertTrue(state is ResponseState.Error<*>)
        assertEquals("Error", state.message)
    }

    @Test
    fun `When expression with all ResponseState types`() {
        val states = listOf<ResponseState<String>>(
            ResponseState.Loading(),
            ResponseState.Success("Success"),
            ResponseState.Error("Error")
        )

        val results = states.map { state ->
            when (state) {
                is ResponseState.Loading<*> -> "Loading"
                is ResponseState.Success<*> -> "Success: ${state.data}"
                is ResponseState.Error<*> -> "Error: ${state.message}"
            }
        }

        assertEquals("Loading", results[0])
        assertEquals("Success: Success", results[1])
        assertEquals("Error: Error", results[2])
    }

    @Test
    fun `ResponseState with complex generic type`() {
        data class PaginatedResponse<T>(val data: List<T>, val cursor: String?)
        data class Stream(val id: String, val name: String)

        val streams = listOf(Stream("1", "Stream1"), Stream("2", "Stream2"))
        val paginatedData = PaginatedResponse(streams, "next_cursor")

        val state = ResponseState.Success(paginatedData)

        assertEquals(paginatedData, state.data)
        assertNotNull(state.data)
        assertEquals(2, state.data!!.data.size)
        assertNotNull(state.data)
        assertEquals("next_cursor", state.data!!.cursor)
    }

    @Test
    fun `ResponseState with Pair type`() {
        val testData = Pair(listOf("a", "b", "c"), "cursor_123")
        val state = ResponseState.Success(testData)

        assertEquals(testData, state.data)
        assertNotNull(state.data)
        assertEquals(3, state.data!!.first.size)
        assertEquals("cursor_123", state.data!!.second)
    }

    // ========== Edge Cases ==========

    @Test
    fun `Loading and Success have different data values`() {
        val loadingData = "Loading..."
        val successData = "Loaded!"

        val loadingState = ResponseState.Loading(loadingData)
        val successState = ResponseState.Success(successData)

        assertNotEquals(loadingState.data, successState.data)
        assertEquals(loadingData, loadingState.data)
        assertEquals(successData, successState.data)
    }

    @Test
    fun `Error with data and Success with same data are different states`() {
        val testData = "Same data"

        val errorState = ResponseState.Error("Error occurred", testData)
        val successState = ResponseState.Success(testData)

        assertEquals(testData, errorState.data)
        assertEquals(testData, successState.data)
        assertNotNull(errorState.message)
        assertNull(successState.message)
        assertFalse(errorState is ResponseState.Success<*>)
        assertFalse(successState is ResponseState.Error<*>)
    }

    @Test
    fun `ResponseState can hold Unit type`() {
        val loadingState = ResponseState.Loading<Unit>()
        val successState = ResponseState.Success(Unit)
        val errorState = ResponseState.Error<Unit>("Error")

        assertNull(loadingState.data)
        assertEquals(Unit, successState.data)
        assertNull(errorState.data)
    }

    @Test
    fun `ResponseState with very large list`() {
        val largeList = (1..10000).toList()
        val state = ResponseState.Success(largeList)

        assertNotNull(state.data)
        assertEquals(10000, state.data!!.size)
        assertNotNull(state.data)
        assertNotNull(state.data)
        assertEquals(1, state.data!!.first())
        assertEquals(10000, state.data!!.last())
    }

    @Test
    fun `Multiple error states with different messages`() {
        val error1 = ResponseState.Error<String>("Network error")
        val error2 = ResponseState.Error<String>("Invalid token")
        val error3 = ResponseState.Error<String>("Server error")

        assertEquals("Network error", error1.message)
        assertEquals("Invalid token", error2.message)
        assertEquals("Server error", error3.message)
    }

    @Test
    fun `ResponseState with nested generics`() {
        val nestedData = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9)
        )
        val state = ResponseState.Success(nestedData)

        assertNotNull(state.data)
        assertEquals(3, state.data!!.size)
        assertEquals(2, state.data!![0][1])
        assertEquals(9, state.data!![2][2])
    }
}
