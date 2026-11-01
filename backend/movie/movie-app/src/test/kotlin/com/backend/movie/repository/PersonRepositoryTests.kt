package com.backend.movie.repository

import com.backend.movie.entities.Credit
import com.backend.movie.entities.CreditId
import com.backend.movie.entities.Movie
import com.backend.movie.entities.Person
import com.backend.movie.enums.CreditType.CAST
import com.backend.movie.service.likePattern
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersonRepositoryTests(
    @Autowired private val movies: MovieRepository,
    @Autowired private val people: PersonRepository,
    @Autowired private val credits: CreditRepository,
) {

    @BeforeTest
    fun emptyTables() {
        movies.deleteAll()
        people.deleteAll()
    }

    private fun person(name: String): UUID = people.save(Person(name = name)).id!!

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
        val movieId = movies.save(Movie(title = "Heat")).id!!
        val pacino = person("Al Pacino")
        credits.save(Credit(CreditId(movieId, pacino, CAST)))

        people.deleteById(pacino)

        assertTrue(credits.findByMovieIds(listOf(movieId)).isEmpty())
    }
}
