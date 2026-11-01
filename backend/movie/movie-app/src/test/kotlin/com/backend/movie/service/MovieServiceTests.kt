package com.backend.movie.service

import com.backend.movie.client.MediaApi
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
import java.util.Optional
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MovieServiceTests {

    private val movieRepository = mockk<MovieRepository>()
    private val mediaApi = mockk<MediaApi>()
    private val service = MovieService(movieRepository, mediaApi)

    private val id = UUID.randomUUID()

    @BeforeTest
    fun setUp() {
        every { movieRepository.save(any()) } answers { firstArg() }
        every { movieRepository.deleteById(any()) } just runs
        every { mediaApi.deleteMovieFiles(any()) } just runs
    }

    @Test
    fun listMovies() {
        every { movieRepository.search(any(), any(), any(), any(), any(), any()) } returns listOf(Movie(id = id, title = "Heat"))
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
        every { movieRepository.findById(id) } returns Optional.of(Movie(id = id, title = "Heat", artworkKey = "old.jpg"))

        val updated = service.updateMovie(id, "Heat (1995)", "A heist", null, 170)

        assertEquals("Heat (1995)", updated.title)
        assertEquals(170, updated.runtimeMinutes)
        assertEquals("old.jpg", updated.artworkKey)
    }

    @Test
    fun unknownIdIsNotFound() {
        every { movieRepository.findById(id) } returns Optional.empty()

        assertFailsWith<NotFoundException> { service.getMovie(id) }
        assertFailsWith<NotFoundException> { service.updateMovie(id, "Heat", null, null, null) }
        assertFailsWith<NotFoundException> { service.attachArtwork(id, "new.jpg") }
        assertFailsWith<NotFoundException> { service.deleteMovie(id) }
        verify(exactly = 0) { movieRepository.save(any()) }
        verify(exactly = 0) { movieRepository.deleteById(any()) }
        verify(exactly = 0) { mediaApi.deleteMovieFiles(any()) }
    }

    @Test
    fun attachArtworkReturnsReplacedKey() {
        every { movieRepository.findById(id) } returns Optional.of(Movie(id = id, title = "Heat", artworkKey = "old.jpg"))

        assertEquals("old.jpg", service.attachArtwork(id, "new.jpg"))
        verify { movieRepository.save(match { it.artworkKey == "new.jpg" }) }
    }

    @Test
    fun attachFirstArtworkReturnsNull() {
        every { movieRepository.findById(id) } returns Optional.of(Movie(id = id, title = "Heat", artworkKey = null))

        assertNull(service.attachArtwork(id, "new.jpg"))
    }

    @Test
    fun deleteMovieRemovesRowThenFiles() {
        every { movieRepository.findById(id) } returns Optional.of(Movie(id = id, title = "Heat"))

        service.deleteMovie(id)

        verifyOrder {
            movieRepository.deleteById(id)
            mediaApi.deleteMovieFiles(id)
        }
    }

    @Test
    fun deleteMovieWhenMediaServiceFails() {
        every { movieRepository.findById(id) } returns Optional.of(Movie(id = id, title = "Heat"))
        every { mediaApi.deleteMovieFiles(id) } throws RuntimeException("media-app is down")

        service.deleteMovie(id)

        verify { movieRepository.deleteById(id) }
    }
}
