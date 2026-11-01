package com.backend.movie.repository

import com.backend.movie.entities.Credit
import com.backend.movie.entities.CreditId
import com.backend.movie.entities.Movie
import com.backend.movie.entities.Person
import com.backend.movie.enums.CreditType
import com.backend.movie.enums.CreditType.CAST
import com.backend.movie.enums.CreditType.CREATOR
import com.backend.movie.service.likePattern
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.dao.OptimisticLockingFailureException
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MovieRepositoryTests(
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

    private fun search(
        title: String = "",
        cast: String = "",
        creator: String = "",
        limit: Int = 20,
        offset: Long = 0,
    ): Pair<List<String>, Long> {
        val found = movies.search(title.trim(), likePattern(title), likePattern(cast), likePattern(creator), limit, offset)
        return found.map { it.title } to movies.countSearch(likePattern(title), likePattern(cast), likePattern(creator))
    }

    @Test
    fun searchByTitleIgnoresCase() {
        movie("Heat")
        movie("Alien")

        assertEquals(listOf("Heat") to 1L, search(title = "HEAT"))
    }

    @Test
    fun searchByCast() {
        val pacino = person("Al Pacino")
        credit(movie("Heat"), pacino, CAST)
        credit(movie("Alien"), pacino, CREATOR)

        assertEquals(listOf("Heat") to 1L, search(cast = "pacino"))
    }

    @Test
    fun searchByCreator() {
        val heat = movie("Heat")
        movie("Alien")
        credit(heat, person("Michael Mann"), CREATOR)

        assertEquals(listOf("Heat") to 1L, search(creator = "mann"))
    }

    @Test
    fun searchByTitleCastAndCreator() {
        val pacino = person("Al Pacino")
        val mann = person("Michael Mann")
        val heat = movie("Heat")
        credit(heat, pacino, CAST)
        credit(heat, mann, CREATOR)
        val heatwave = movie("Heatwave")
        credit(heatwave, pacino, CAST)
        val insider = movie("The Insider")
        credit(insider, pacino, CAST)
        credit(insider, mann, CREATOR)

        assertEquals(listOf("Heat") to 1L, search(title = "heat", cast = "pacino", creator = "mann"))
    }

    @Test
    fun searchReturnsMovieOnceForTwoMatchingCastMembers() {
        val movie = movie("Zodiac")
        credit(movie, person("David Fincher"), CAST)
        credit(movie, person("David Lynch"), CAST)

        assertEquals(listOf("Zodiac") to 1L, search(cast = "david"))
    }

    @Test
    fun searchTreatsPercentSignAsText() {
        movie("100% Wolf")
        movie("1000 Ways")

        assertEquals(listOf("100% Wolf") to 1L, search(title = "100%"))
    }

    @Test
    fun searchPagesWithoutRepeats() {
        // The same title three times, so only the id keeps the order stable.
        repeat(3) { movie("Heat") }

        val firstPage = movies.search("heat", "%heat%", "", "", 2, 0).map { it.id }
        val secondPage = movies.search("heat", "%heat%", "", "", 2, 2).map { it.id }

        assertEquals(2, firstPage.size)
        assertEquals(1, secondPage.size)
        assertEquals(3, (firstPage + secondPage).toSet().size)
        assertEquals(3L, movies.countSearch("%heat%", "", ""))
    }

    @Test
    fun deleteByIdRemovesCredits() {
        val movie = movie("Heat")
        val pacino = person("Al Pacino")
        credit(movie, pacino, CAST)

        movies.deleteById(movie)

        assertTrue(credits.findByPersonIds(listOf(pacino)).isEmpty())
    }

    @Test
    fun staleSaveFails() {
        val read = movies.findById(movie("Heat")).get()
        movies.save(read.copy(title = "Heat (1995)"))

        assertFailsWith<OptimisticLockingFailureException> { movies.save(read.copy(title = "Heat (stale)")) }
    }
}
