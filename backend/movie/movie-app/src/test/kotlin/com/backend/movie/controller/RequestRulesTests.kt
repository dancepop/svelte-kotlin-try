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

class RequestRulesTests {

    private val id = UUID.randomUUID().toString()

    private suspend fun violationsOf(request: Message): Set<String> {
        val error = assertFailsWith<StatusException> { rpc(request) { error("The request should have been rejected") } }
        assertEquals(Status.Code.INVALID_ARGUMENT, error.status.code)
        return error.status.description.orEmpty().split("; ").toSet()
    }

    private suspend fun assertRejected(request: Message) {
        violationsOf(request)
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
        assertEquals(setOf("Title is required"), violationsOf(movie(title = "  ")))
    }

    @Test
    fun titleTooLong() = runTest {
        assertEquals(setOf("Title must be 200 characters or fewer"), violationsOf(movie(title = "a".repeat(201))))
    }

    @Test
    fun synopsisTooLong() = runTest {
        assertEquals(
            setOf("Synopsis must be 5000 characters or fewer"),
            violationsOf(movie(synopsis = "a".repeat(5001))),
        )
    }

    @Test
    fun releaseDateWrongFormat() = runTest {
        assertEquals(
            setOf("Release date must be a real date in the format YYYY-MM-DD"),
            violationsOf(movie(releaseDate = "2025-1-5")),
        )
    }

    @Test
    fun runtimeOutOfRange() = runTest {
        val expected = setOf("Runtime must be 1 to 1000 minutes")

        assertEquals(expected, violationsOf(movie(runtimeMinutes = 0)))
        assertEquals(expected, violationsOf(movie(runtimeMinutes = 1001)))
    }

    @Test
    fun everyBrokenRuleReported() = runTest {
        val request = movie(title = "", synopsis = "a".repeat(5001), releaseDate = "soon", runtimeMinutes = 0)

        assertEquals(4, violationsOf(request).size)
    }

    @Test
    fun missingFields() = runTest {
        assertRejected(createMovieRequest {})
    }

    @Test
    fun personNameAndBio() = runTest {
        assertEquals(setOf("Name is required"), violationsOf(person(name = " ")))
        assertEquals(setOf("Name must be 200 characters or fewer"), violationsOf(person(name = "a".repeat(201))))
        assertEquals(setOf("Bio must be 5000 characters or fewer"), violationsOf(person(bio = "a".repeat(5001))))
    }

    @Test
    fun malformedId() = runTest {
        assertRejected(getMovieRequest { id = "42" })
    }

    @Test
    fun pagingOutOfRange() = runTest {
        assertRejected(listMoviesRequest { page = -1; pageSize = 20 })
        assertRejected(listMoviesRequest { pageSize = 0 })
        assertRejected(listMoviesRequest { pageSize = 101 })
    }

    @Test
    fun creditTypeNotSet() = runTest {
        val request = creditRequest {
            movieId = id
            personId = id
            type = CreditType.CREDIT_TYPE_UNSPECIFIED
        }

        assertRejected(request)
    }

    @Test
    fun tooManyIds() = runTest {
        val request = listCreditsByMoviesRequest { movieIds += List(101) { id } }

        assertRejected(request)
    }
}
