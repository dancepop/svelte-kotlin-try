package com.backend.movie.service

import com.backend.movie.client.MediaClient
import com.backend.movie.entities.Movie
import com.backend.movie.exception.NotFoundException
import com.backend.movie.model.MovieFilter
import com.backend.movie.model.Page
import com.backend.movie.repository.MovieRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class MovieService(
    private val movieRepository: MovieRepository,
    private val mediaClient: MediaClient,
) {
    private val logger = LoggerFactory.getLogger(MovieService::class.java)

    @Transactional(readOnly = true)
    fun listMovies(filter: MovieFilter): Page<Movie> {
        val titlePattern = likePattern(filter.query)
        val castPattern = likePattern(filter.castName)
        val creatorPattern = likePattern(filter.creatorName)
        val movies = movieRepository.search(
            filter.query.trim(), titlePattern, castPattern, creatorPattern,
            filter.pageSize, offsetOf(filter.page, filter.pageSize),
        )
        return Page(movies, movieRepository.countSearch(titlePattern, castPattern, creatorPattern))
    }

    @Transactional(readOnly = true)
    fun getMovie(id: UUID): Movie = findMovie(id)

    @Transactional
    fun createMovie(title: String, synopsis: String?, releaseDate: LocalDate?, runtimeMinutes: Int?): Movie {
        val movie = Movie(
            title = title.trim(),
            synopsis = synopsis,
            releaseDate = releaseDate,
            runtimeMinutes = runtimeMinutes,
        )
        return movieRepository.save(movie)
    }

    @Transactional
    fun updateMovie(id: UUID, title: String, synopsis: String?, releaseDate: LocalDate?, runtimeMinutes: Int?): Movie {
        val changed = findMovie(id).copy(
            title = title.trim(),
            synopsis = synopsis,
            releaseDate = releaseDate,
            runtimeMinutes = runtimeMinutes,
            updatedAt = Instant.now(),
        )
        return movieRepository.save(changed)
    }

    /** Called by media-service once artwork is stored. Returns the replaced file's key, for it to delete. */
    @Transactional
    fun attachArtwork(movieId: UUID, storageKey: String): String? {
        val movie = findMovie(movieId)
        movieRepository.save(movie.copy(artworkKey = storageKey, updatedAt = Instant.now()))
        return movie.artworkKey
    }

    // Not @Transactional: deleteById commits on its own, so the row is gone before the files are touched.
    fun deleteMovie(id: UUID) {
        findMovie(id)
        // credits go with it through the cascade.
        movieRepository.deleteById(id)
        // If media-service is down, the movie is still deleted and the folder is left over.
        try {
            mediaClient.deleteMovieFiles(id)
        } catch (e: Exception) {
            logger.warn("Couldn't delete the files of deleted movie {}", id, e)
        }
    }

    private fun findMovie(id: UUID): Movie =
        movieRepository.findByIdOrNull(id) ?: throw NotFoundException("This movie no longer exists")
}
