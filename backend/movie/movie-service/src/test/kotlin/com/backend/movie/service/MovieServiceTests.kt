package com.backend.movie.service

import com.backend.movie.client.MediaClient
import com.backend.movie.entities.Movie
import com.backend.movie.exception.NotFoundException
import com.backend.movie.model.MovieFilter
import com.backend.movie.repository.MovieRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MovieServiceTests {

    private val movieRepository = mockk<MovieRepository>()
    private val mediaClient = mockk<MediaClient>()
    private val service = MovieService(movieRepository, mediaClient)

    private val id = UUID.randomUUID()
    private val lastUpdate = Instant.parse("2025-01-01T00:00:00Z")

    private fun stored(artworkKey: String? = null) =
        Movie(id = id, title = "Heat", artworkKey = artworkKey, updatedAt = lastUpdate)

    @BeforeTest
    fun setUp() {
        // Like the real repository, save returns the movie it was given.
        every { movieRepository.save(any()) } answers { firstArg() }
        every { movieRepository.deleteById(any()) } just runs
        every { mediaClient.deleteMovieFiles(any()) } just runs
    }

    // findByIdOrNull is a Kotlin extension that calls findById, so that's what gets mocked.
    private fun givenStored(movie: Movie?) {
        every { movieRepository.findById(id) } returns Optional.ofNullable(movie)
    }

    @Test
    fun listMovies() {
        every { movieRepository.search(any(), any(), any(), any(), any(), any()) } returns listOf(stored())
        every { movieRepository.countSearch(any(), any(), any()) } returns 41

        val page = service.listMovies(MovieFilter(query = " heat ", castName = "david", page = 2, pageSize = 20))

        verify { movieRepository.search("heat", "%heat%", "%david%", "", 20, 40L) }
        verify { movieRepository.countSearch("%heat%", "%david%", "") }
        assertEquals(41, page.totalCount)
    }

    @Test
    fun createMovieTrimsTitle() {
        val created = service.createMovie("  Heat  ", null, null, null)

        assertEquals("Heat", created.title)
    }

    @Test
    fun updateMovieKeepsArtwork() {
        givenStored(stored(artworkKey = "old.jpg"))

        val updated = service.updateMovie(id, "Heat (1995)", "A heist", null, 170)

        assertEquals("Heat (1995)", updated.title)
        assertEquals(170, updated.runtimeMinutes)
        assertEquals("old.jpg", updated.artworkKey)
        assertTrue(updated.updatedAt > lastUpdate)
    }

    @Test
    fun unknownIdIsNotFound() {
        givenStored(null)

        assertFailsWith<NotFoundException> { service.getMovie(id) }
        assertFailsWith<NotFoundException> { service.updateMovie(id, "Heat", null, null, null) }
        assertFailsWith<NotFoundException> { service.attachArtwork(id, "new.jpg") }
        assertFailsWith<NotFoundException> { service.deleteMovie(id) }
        verify(exactly = 0) { movieRepository.save(any()) }
        verify(exactly = 0) { movieRepository.deleteById(any()) }
        verify(exactly = 0) { mediaClient.deleteMovieFiles(any()) }
    }

    @Test
    fun attachArtworkReturnsReplacedKey() {
        givenStored(stored(artworkKey = "old.jpg"))

        assertEquals("old.jpg", service.attachArtwork(id, "new.jpg"))
        verify { movieRepository.save(match { it.artworkKey == "new.jpg" }) }
    }

    @Test
    fun attachFirstArtworkReturnsNull() {
        givenStored(stored(artworkKey = null))

        assertNull(service.attachArtwork(id, "new.jpg"))
    }

    @Test
    fun deleteMovieRemovesRowThenFiles() {
        givenStored(stored())

        service.deleteMovie(id)

        verifyOrder {
            movieRepository.deleteById(id)
            mediaClient.deleteMovieFiles(id)
        }
    }

    @Test
    fun deleteMovieWhenMediaServiceFails() {
        givenStored(stored())
        every { mediaClient.deleteMovieFiles(id) } throws RuntimeException("media-service is down")

        service.deleteMovie(id)

        verify { movieRepository.deleteById(id) }
    }
}
