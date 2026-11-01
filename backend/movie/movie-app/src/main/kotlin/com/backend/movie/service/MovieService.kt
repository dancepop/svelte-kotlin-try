package com.backend.movie.service

import com.backend.movie.client.MediaApi
import com.backend.movie.entities.Movie
import com.backend.movie.exception.NotFoundException
import com.backend.movie.model.MovieFilter
import com.backend.movie.model.Page
import com.backend.movie.repository.MovieRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class MovieService(
    private val movieRepository: MovieRepository,
    private val mediaApi: MediaApi,
) {
    private val logger = LoggerFactory.getLogger(MovieService::class.java)

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

    fun getMovie(id: UUID): Movie = findMovie(id)

    fun createMovie(title: String, synopsis: String?, releaseDate: LocalDate?, runtimeMinutes: Int?): Movie {
        val movie = Movie(
            title = title.trim(),
            synopsis = synopsis,
            releaseDate = releaseDate,
            runtimeMinutes = runtimeMinutes,
        )
        return movieRepository.save(movie)
    }

    fun updateMovie(id: UUID, title: String, synopsis: String?, releaseDate: LocalDate?, runtimeMinutes: Int?): Movie {
        val changed = findMovie(id).copy(
            title = title.trim(),
            synopsis = synopsis,
            releaseDate = releaseDate,
            runtimeMinutes = runtimeMinutes,
        )
        return movieRepository.save(changed)
    }

    // Media service stores the uploaded file then sends the storageKey (file location)
    fun attachArtwork(movieId: UUID, storageKey: String): String? {
        val movie = findMovie(movieId)
        movieRepository.save(movie.copy(artworkKey = storageKey))
        return movie.artworkKey
    }

    // Intentionally not transactional cause imo more important in this domain that the row is gone.
    // Files can just delete at a later time.
    fun deleteMovie(id: UUID) {
        findMovie(id)
        movieRepository.deleteById(id)
        try {
            mediaApi.deleteMovieFiles(id)
        } catch (e: Exception) {
            logger.warn("Couldn't delete the files of deleted movie {}", id, e)
        }
    }

    private fun findMovie(id: UUID): Movie =
        movieRepository.findByIdOrNull(id) ?: throw NotFoundException("This movie no longer exists")
}
