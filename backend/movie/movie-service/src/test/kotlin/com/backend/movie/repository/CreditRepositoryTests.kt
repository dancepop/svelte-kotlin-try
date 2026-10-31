package com.backend.movie.repository

import com.backend.movie.enums.CreditType.CAST
import com.backend.movie.enums.CreditType.CREATOR
import com.backend.movie.model.MovieCreditRow
import com.backend.movie.model.PersonCreditRow
import org.springframework.dao.DataIntegrityViolationException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// A failed INSERT aborts the test's transaction, so the tests that expect one stop right after it.
class CreditRepositoryTests : RepositoryTestBase() {

    @Test
    fun insertAndDelete() {
        val movie = movie("Heat")
        val pacino = person("Al Pacino")

        assertEquals(1, credits.insert(movie, pacino, "CAST"))
        assertEquals(1, credits.findByMovieIds(listOf(movie)).size)

        assertEquals(1, credits.delete(movie, pacino, "CAST"))
        assertTrue(credits.findByMovieIds(listOf(movie)).isEmpty())
    }

    @Test
    fun insertSamePersonAsCastAndCreator() {
        val movie = movie("Unforgiven")
        val eastwood = person("Clint Eastwood")
        credit(movie, eastwood, CAST)
        credit(movie, eastwood, CREATOR)

        val types = credits.findByMovieIds(listOf(movie)).map { it.creditType }

        assertEquals(setOf(CAST, CREATOR), types.toSet())
    }

    // CreditService turns these constraint names into messages, so the names must be in the error.
    @Test
    fun insertDuplicateNamesPrimaryKey() {
        val movie = movie("Heat")
        val pacino = person("Al Pacino")
        credit(movie, pacino, CAST)

        val error = assertFailsWith<DataIntegrityViolationException> { credit(movie, pacino, CAST) }

        assertContains(error.mostSpecificCause.message.orEmpty(), "pk_credits")
    }

    @Test
    fun insertUnknownMovieNamesForeignKey() {
        val pacino = person("Al Pacino")

        val error = assertFailsWith<DataIntegrityViolationException> {
            credits.insert(UUID.randomUUID(), pacino, "CAST")
        }

        assertContains(error.mostSpecificCause.message.orEmpty(), "fk_credits_movie_id")
    }

    @Test
    fun insertUnknownPersonNamesForeignKey() {
        val movie = movie("Heat")

        val error = assertFailsWith<DataIntegrityViolationException> {
            credits.insert(movie, UUID.randomUUID(), "CAST")
        }

        assertContains(error.mostSpecificCause.message.orEmpty(), "fk_credits_person_id")
    }

    @Test
    fun findByMovieIdsAndPersonIds() {
        val heat = movie("Heat")
        val alien = movie("Alien")
        val pacino = person("Al Pacino")
        val weaver = person("Sigourney Weaver")
        credit(heat, pacino, CAST)
        credit(alien, weaver, CAST)

        assertEquals(
            listOf(
                MovieCreditRow(heat, pacino, "Al Pacino", CAST),
                MovieCreditRow(alien, weaver, "Sigourney Weaver", CAST),
            ),
            credits.findByMovieIds(listOf(heat, alien)),
        )
        assertEquals(
            listOf(
                PersonCreditRow(weaver, alien, "Alien", CAST),
                PersonCreditRow(pacino, heat, "Heat", CAST),
            ),
            credits.findByPersonIds(listOf(pacino, weaver)),
        )
    }
}
