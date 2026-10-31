package com.backend.movie.repository

import com.backend.movie.enums.CreditType.CAST
import com.backend.movie.service.likePattern
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersonRepositoryTests : RepositoryTestBase() {

    @Test
    fun searchByPartOfNameIgnoresCase() {
        person("Amy Adams")
        person("Amy Poehler")
        person("Al Pacino")

        val found = people.search("amy", likePattern("amy"), 20, 0).map { it.name }

        assertEquals(listOf("Amy Adams", "Amy Poehler"), found.sorted())
        assertEquals(2L, people.countSearch(likePattern("amy")))
    }

    @Test
    fun searchWithEmptyPatternReturnsEveryone() {
        person("Amy Adams")
        person("Al Pacino")

        assertEquals(2, people.search("", "", 20, 0).size)
        assertEquals(2L, people.countSearch(""))
    }

    @Test
    fun deleteByIdRemovesCredits() {
        val movie = movie("Heat")
        val pacino = person("Al Pacino")
        credit(movie, pacino, CAST)

        people.deleteById(pacino)

        assertTrue(credits.findByMovieIds(listOf(movie)).isEmpty())
    }
}
