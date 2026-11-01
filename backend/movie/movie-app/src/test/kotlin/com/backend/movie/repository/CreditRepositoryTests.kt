package com.backend.movie.repository

import com.backend.movie.entities.Credit
import com.backend.movie.entities.CreditId
import com.backend.movie.entities.Movie
import com.backend.movie.entities.Person
import com.backend.movie.enums.CreditType
import com.backend.movie.enums.CreditType.CAST
import com.backend.movie.enums.CreditType.CREATOR
import com.backend.movie.model.MovieCreditRow
import com.backend.movie.model.PersonCreditRow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.dao.DataIntegrityViolationException
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CreditRepositoryTests(
    @Autowired private val movies: MovieRepository,
    @Autowired private val people: PersonRepository,
    @Autowired private val credits: CreditRepository,
) {

    @BeforeTest
    fun emptyTables() {
        movies.deleteAll()
        people.deleteAll()
    }

    private fun movie(title: String): UUID = movies.save(Movie(title = title)).id!!
    private fun person(name: String): UUID = people.save(Person(name = name)).id!!
    private fun credit(movieId: UUID, personId: UUID, type: CreditType) {
        credits.save(Credit(CreditId(movieId, personId, type)))
    }

    @Test
    fun insertAndDelete() {
        val movie = movie("Heat")
        val pacino = person("Al Pacino")
        val id = CreditId(movie, pacino, CAST)

        credits.save(Credit(id))
        assertEquals(1, credits.findByMovieIds(listOf(movie)).size)

        credits.deleteById(id)
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
            credit(UUID.randomUUID(), pacino, CAST)
        }

        assertContains(error.mostSpecificCause.message.orEmpty(), "fk_credits_movie_id")
    }

    @Test
    fun insertUnknownPersonNamesForeignKey() {
        val movie = movie("Heat")

        val error = assertFailsWith<DataIntegrityViolationException> {
            credit(movie, UUID.randomUUID(), CAST)
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
