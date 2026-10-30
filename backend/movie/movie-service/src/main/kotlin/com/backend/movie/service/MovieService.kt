package com.backend.movie.service

import com.backend.movie.client.MediaClient
import com.backend.movie.entities.Movie
import com.backend.movie.enums.MediaKind
import com.backend.movie.enums.MovieStatus
import com.backend.movie.exception.ConflictException
import com.backend.movie.exception.NotFoundException
import com.backend.movie.exception.PublishedMovieException
import com.backend.movie.model.MovieFilter
import com.backend.movie.model.MovieWithMedia
import com.backend.movie.model.Page
import com.backend.movie.repository.MovieMediaRepository
import com.backend.movie.repository.MovieRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private const val STALE_MOVIE = "Someone else changed this movie. Reload to see their changes."
private const val MISSING_MOVIE = "This movie no longer exists"

@Service
class MovieService(
    private val movieRepository: MovieRepository,
    private val movieMediaRepository: MovieMediaRepository,
    private val mediaClient: MediaClient,
    private val transaction: TransactionTemplate,
) {
    private val logger = LoggerFactory.getLogger(MovieService::class.java)

    @Transactional(readOnly = true)
    fun listMovies(filter: MovieFilter): Page<MovieWithMedia> {
        requireValid(pagingErrors(filter.page, filter.pageSize))
        val titlePattern = likePattern(filter.query)
        val castPattern = likePattern(filter.castName)
        val creatorPattern = likePattern(filter.creatorName)
        val status = filter.status.name
        val movies = movieRepository.search(
            filter.query.trim(), titlePattern, castPattern, creatorPattern, status,
            filter.pageSize, offsetOf(filter.page, filter.pageSize),
        )
        val total = movieRepository.countSearch(titlePattern, castPattern, creatorPattern, status)
        return Page(withMedia(movies), total)
    }

    @Transactional(readOnly = true)
    fun getMovie(id: UUID): MovieWithMedia = withMedia(findMovie(id))

    @Transactional
    fun createMovie(title: String, synopsis: String?, releaseDate: String?, runtimeMinutes: Int?): MovieWithMedia {
        requireValid(movieFieldErrors(title, synopsis, releaseDate, runtimeMinutes))
        val movie = Movie(
            title = title.trim(),
            synopsis = synopsis,
            releaseDate = releaseDate?.let(LocalDate::parse),
            runtimeMinutes = runtimeMinutes,
        )
        return MovieWithMedia(movieRepository.save(movie), emptyList())
    }

    @Transactional
    fun updateMovie(
        id: UUID,
        version: Int,
        title: String,
        synopsis: String?,
        releaseDate: String?,
        runtimeMinutes: Int?,
    ): MovieWithMedia {
        requireValid(movieFieldErrors(title, synopsis, releaseDate, runtimeMinutes))
        val movie = findMovie(id)
        // The status comes before the version: reloading a stale form wouldn't help until it's unpublished.
        requireDraft(movie)
        val changed = movie.copy(
            title = title.trim(),
            synopsis = synopsis,
            releaseDate = releaseDate?.let(LocalDate::parse),
            runtimeMinutes = runtimeMinutes,
        )
        return withMedia(saveWithVersion(changed, version))
    }

    @Transactional
    fun publishMovie(id: UUID, version: Int): MovieWithMedia = changeStatus(id, version, MovieStatus.PUBLISHED)

    @Transactional
    fun unpublishMovie(id: UUID, version: Int): MovieWithMedia = changeStatus(id, version, MovieStatus.DRAFT)

    /** Called by media-service once a file is stored. Returns the replaced file's key, for it to delete. */
    @Transactional
    fun attachMedia(
        movieId: UUID,
        kind: MediaKind,
        storageKey: String,
        contentType: String,
        sizeBytes: Long,
        durationSeconds: Int?,
    ): String? {
        requireValid(durationErrors(kind, durationSeconds))
        movieRepository.lockDraft(movieId)
        return movieMediaRepository.upsert(movieId, kind.name, storageKey, contentType, sizeBytes, durationSeconds)
    }

    fun removeMedia(movieId: UUID, kind: MediaKind): MovieWithMedia {
        val (movie, removedKey) = transaction.execute {
            val movie = movieRepository.lockDraft(movieId)
            movie to movieMediaRepository.deleteByMovieIdAndKind(movieId, kind.name)
        }
        // After the commit: no row points at the file any more, so a failure here only leaves an orphan.
        if (removedKey != null) deleteFileQuietly(removedKey)
        return withMedia(movie)
    }

    fun deleteMovie(id: UUID, version: Int) {
        transaction.executeWithoutResult {
            val movie = movieRepository.lockDraft(id)
            try {
                // credits and movie_media go with it through the cascade.
                movieRepository.delete(movie.copy(version = version))
            } catch (_: OptimisticLockingFailureException) {
                throw ConflictException(STALE_MOVIE)
            }
        }
        // After the commit: if media-service is down, the movie is still deleted and the folder is left over.
        try {
            mediaClient.deleteMovieFiles(id)
        } catch (e: Exception) {
            logger.warn("Couldn't delete the files of deleted movie {}", id, e)
        }
    }

    private fun changeStatus(id: UUID, version: Int, status: MovieStatus): MovieWithMedia {
        val movie = findMovie(id)
        // Comes before the version check, so a double click shows no error.
        if (movie.status == status) return withMedia(movie)
        return withMedia(saveWithVersion(movie.copy(status = status), version))
    }

    // Saving the version the client sent makes Spring Data run UPDATE … WHERE version = ?.
    private fun saveWithVersion(movie: Movie, version: Int): Movie = try {
        movieRepository.save(movie.copy(updatedAt = Instant.now(), version = version))
    } catch (_: OptimisticLockingFailureException) {
        throw ConflictException(STALE_MOVIE)
    }

    private fun deleteFileQuietly(storageKey: String) {
        try {
            mediaClient.deleteFile(storageKey)
        } catch (e: Exception) {
            logger.warn("Couldn't delete removed file {}", storageKey, e)
        }
    }

    private fun findMovie(id: UUID): Movie =
        movieRepository.findByIdOrNull(id) ?: throw NotFoundException(MISSING_MOVIE)

    private fun withMedia(movie: Movie): MovieWithMedia = withMedia(listOf(movie)).single()

    // One query for the whole page, matched to the movies in memory.
    private fun withMedia(movies: List<Movie>): List<MovieWithMedia> {
        // An empty list would produce invalid SQL: IN ()
        if (movies.isEmpty()) return emptyList()
        val media = movieMediaRepository.findByMovieIds(movies.map { it.id!! }).groupBy { it.id.movieId }
        return movies.map { MovieWithMedia(it, media[it.id].orEmpty()) }
    }
}

/** Locks the movie's row for the rest of the transaction and checks it's a draft, so it can't be published meanwhile. */
internal fun MovieRepository.lockDraft(id: UUID): Movie {
    val movie = findByIdForUpdate(id) ?: throw NotFoundException(MISSING_MOVIE)
    requireDraft(movie)
    return movie
}

private fun requireDraft(movie: Movie) {
    if (movie.status != MovieStatus.DRAFT) throw PublishedMovieException()
}
