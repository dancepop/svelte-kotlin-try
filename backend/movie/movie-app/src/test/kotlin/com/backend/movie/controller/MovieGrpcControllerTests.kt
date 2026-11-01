package com.backend.movie.controller

import com.backend.movie.api.attachArtworkRequest
import com.backend.movie.api.createMovieRequest
import com.backend.movie.api.listMoviesRequest
import com.backend.movie.api.movieFields
import com.backend.movie.entities.Movie
import com.backend.movie.model.MovieFilter
import com.backend.movie.model.Page
import com.backend.movie.service.MovieService
import io.grpc.Status
import io.grpc.StatusException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

// The service is mocked, so each test checks the proto → Kotlin conversion on the way in
// and the Kotlin → proto conversion on the way out.
class MovieGrpcControllerTests {

    private val movieService = mockk<MovieService>()
    private val controller = MovieGrpcController(movieService)

    private val id = UUID.randomUUID()

    @Test
    fun listMoviesPassesFilterAndReturnsPage() = runTest {
        every { movieService.listMovies(any()) } returns Page(listOf(Movie(id = id, title = "Heat")), 41)

        val response = controller.listMovies(listMoviesRequest { query = "heat"; castName = "pacino"; page = 2; pageSize = 20 })

        verify { movieService.listMovies(MovieFilter(query = "heat", castName = "pacino", page = 2, pageSize = 20)) }
        assertEquals(listOf("Heat"), response.moviesList.map { it.title })
        assertEquals(41, response.totalCount)
    }

    @Test
    fun createMovieWithOnlyTitle() = runTest {
        every { movieService.createMovie("Heat", null, null, null) } returns Movie(id = id, title = "Heat")

        val response = controller.createMovie(createMovieRequest { fields = movieFields { title = "Heat" } })

        assertEquals(id.toString(), response.id)
        assertFalse(response.hasSynopsis())
        assertFalse(response.hasReleaseDate())
        assertFalse(response.hasRuntimeMinutes())
        assertFalse(response.hasArtworkKey())
    }

    @Test
    fun createMovieWithEveryField() = runTest {
        val releaseDate = LocalDate.of(1995, 12, 15)
        every { movieService.createMovie("Heat", "A heist", releaseDate, 170) } returns
            Movie(id = id, title = "Heat", synopsis = "A heist", releaseDate = releaseDate, runtimeMinutes = 170, artworkKey = "a/b")

        val response = controller.createMovie(
            createMovieRequest {
                fields = movieFields { title = "Heat"; synopsis = "A heist"; this.releaseDate = "1995-12-15"; runtimeMinutes = 170 }
            },
        )

        assertEquals("A heist", response.synopsis)
        assertEquals("1995-12-15", response.releaseDate)
        assertEquals(170, response.runtimeMinutes)
        assertEquals("a/b", response.artworkKey)
    }

    @Test
    fun createMovieWithEmptySynopsisKeepsIt() = runTest {
        every { movieService.createMovie("Heat", "", null, null) } returns Movie(id = id, title = "Heat", synopsis = "")

        controller.createMovie(createMovieRequest { fields = movieFields { title = "Heat"; synopsis = "" } })

        verify { movieService.createMovie("Heat", "", null, null) }
    }

    @Test
    fun impossibleReleaseDateIsInvalidArgument() = runTest {
        val error = assertFailsWith<StatusException> {
            controller.createMovie(createMovieRequest { fields = movieFields { title = "Heat"; releaseDate = "2025-02-30" } })
        }

        assertEquals(Status.Code.INVALID_ARGUMENT, error.status.code)
        assertEquals("Release date must be a real date in the format YYYY-MM-DD", error.status.description)
        verify(exactly = 0) { movieService.createMovie(any(), any(), any(), any()) }
    }

    @Test
    fun attachArtworkReturnsReplacedKey() = runTest {
        every { movieService.attachArtwork(id, "new") } returns "old"

        val response = controller.attachArtwork(attachArtworkRequest { movieId = id.toString(); storageKey = "new" })

        assertEquals("old", response.replacedStorageKey)
    }

    @Test
    fun attachFirstArtworkReturnsNoKey() = runTest {
        every { movieService.attachArtwork(id, "new") } returns null

        val response = controller.attachArtwork(attachArtworkRequest { movieId = id.toString(); storageKey = "new" })

        assertFalse(response.hasReplacedStorageKey())
    }
}
