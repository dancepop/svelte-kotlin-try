package com.backend.movie.controller

import com.backend.movie.api.CreditType
import com.backend.movie.api.createMovieRequest
import com.backend.movie.api.createPersonRequest
import com.backend.movie.api.creditRequest
import com.backend.movie.api.getMovieRequest
import com.backend.movie.api.listCreditsByMoviesRequest
import com.backend.movie.api.listMoviesRequest
import com.backend.movie.api.movieFields
import com.backend.movie.api.personFields
import com.google.protobuf.Message
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The rules written on the .proto fields, checked through rpc() the way every controller method runs them.
 * protovalidate compiles each CEL rule the first time it sees the message, so a typo in one fails here.
 */
class RequestRulesTests {

    private val id = UUID.randomUUID().toString()

    // The block throws, so a request that wrongly passes fails the test with INTERNAL instead.
    private suspend fun violationsOf(request: Message): Map<String, String> {
        val error = assertFailsWith<StatusException> { rpc(request) { error("The request should have been rejected") } }
        assertEquals(Status.Code.INVALID_ARGUMENT, error.status.code)
        return fieldViolationsOf(error)
    }

    private fun movie(
        title: String = "Heat",
        synopsis: String? = null,
        releaseDate: String? = null,
        runtimeMinutes: Int? = null,
    ) = createMovieRequest {
        fields = movieFields {
            this.title = title
            synopsis?.let { this.synopsis = it }
            releaseDate?.let { this.releaseDate = it }
            runtimeMinutes?.let { this.runtimeMinutes = it }
        }
    }

    private fun person(name: String = "Amy Adams", bio: String? = null) = createPersonRequest {
        fields = personFields {
            this.name = name
            bio?.let { this.bio = it }
        }
    }

    @Test
    fun validAtEveryLimit() = runTest {
        val movie = movie(title = "a".repeat(200), synopsis = "a".repeat(5000), releaseDate = "2024-02-29", runtimeMinutes = 1000)
        val person = person(name = "a".repeat(200), bio = "a".repeat(5000))

        assertEquals("ran", rpc(movie) { "ran" })
        assertEquals("ran", rpc(person) { "ran" })
    }

    @Test
    fun blankTitle() = runTest {
        assertEquals(mapOf("title" to "Title is required"), violationsOf(movie(title = "  ")))
    }

    @Test
    fun titleTooLong() = runTest {
        assertEquals(mapOf("title" to "Title must be 200 characters or fewer"), violationsOf(movie(title = "a".repeat(201))))
    }

    @Test
    fun synopsisTooLong() = runTest {
        assertEquals(
            mapOf("synopsis" to "Synopsis must be 5000 characters or fewer"),
            violationsOf(movie(synopsis = "a".repeat(5001))),
        )
    }

    @Test
    fun releaseDateWrongFormat() = runTest {
        assertEquals(
            mapOf("releaseDate" to "Release date must be a real date in the format YYYY-MM-DD"),
            violationsOf(movie(releaseDate = "2025-1-5")),
        )
    }

    @Test
    fun runtimeOutOfRange() = runTest {
        val expected = mapOf("runtimeMinutes" to "Runtime must be 1 to 1000 minutes")

        assertEquals(expected, violationsOf(movie(runtimeMinutes = 0)))
        assertEquals(expected, violationsOf(movie(runtimeMinutes = 1001)))
    }

    @Test
    fun everyBrokenRuleReported() = runTest {
        val request = movie(title = "", synopsis = "a".repeat(5001), releaseDate = "soon", runtimeMinutes = 0)

        assertEquals(setOf("title", "synopsis", "releaseDate", "runtimeMinutes"), violationsOf(request).keys)
    }

    @Test
    fun missingFields() = runTest {
        assertEquals(setOf("fields"), violationsOf(createMovieRequest {}).keys)
    }

    @Test
    fun personNameAndBio() = runTest {
        assertEquals(mapOf("name" to "Name is required"), violationsOf(person(name = " ")))
        assertEquals(mapOf("name" to "Name must be 200 characters or fewer"), violationsOf(person(name = "a".repeat(201))))
        assertEquals(mapOf("bio" to "Bio must be 5000 characters or fewer"), violationsOf(person(bio = "a".repeat(5001))))
    }

    @Test
    fun malformedId() = runTest {
        assertEquals(setOf("id"), violationsOf(getMovieRequest { id = "42" }).keys)
    }

    @Test
    fun pagingOutOfRange() = runTest {
        assertEquals(setOf("page"), violationsOf(listMoviesRequest { page = -1; pageSize = 20 }).keys)
        assertEquals(setOf("pageSize"), violationsOf(listMoviesRequest { pageSize = 0 }).keys)
        assertEquals(setOf("pageSize"), violationsOf(listMoviesRequest { pageSize = 101 }).keys)
    }

    @Test
    fun creditTypeNotSet() = runTest {
        val request = creditRequest {
            movieId = id
            personId = id
            type = CreditType.CREDIT_TYPE_UNSPECIFIED
        }

        assertEquals(setOf("type"), violationsOf(request).keys)
    }

    @Test
    fun tooManyIds() = runTest {
        val request = listCreditsByMoviesRequest { movieIds += List(101) { id } }

        assertEquals(setOf("movieIds"), violationsOf(request).keys)
    }
}
