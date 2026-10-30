package com.backend.movie.repository

import com.backend.movie.entities.MovieMedia
import com.backend.movie.entities.MovieMediaId
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.Repository
import java.util.UUID

interface MovieMediaRepository : Repository<MovieMedia, MovieMediaId> {

    @Query("SELECT * FROM movie_media WHERE movie_id IN (:movieIds)")
    fun findByMovieIds(movieIds: Collection<UUID>): List<MovieMedia>

    // Postgres 18's RETURNING old.… gives the replaced key in the same statement; null if nothing was replaced.
    @Query(
        """
        INSERT INTO movie_media (movie_id, kind, storage_key, content_type, size_bytes, duration_seconds)
        VALUES (:movieId, :kind, :storageKey, :contentType, :sizeBytes, :durationSeconds)
        ON CONFLICT (movie_id, kind) DO UPDATE
          SET storage_key = excluded.storage_key, content_type = excluded.content_type,
              size_bytes = excluded.size_bytes, duration_seconds = excluded.duration_seconds,
              updated_at = now()
        RETURNING old.storage_key
        """,
    )
    fun upsert(
        movieId: UUID,
        kind: String,
        storageKey: String,
        contentType: String,
        sizeBytes: Long,
        durationSeconds: Int?,
    ): String?

    // Returns the removed key, so there's no separate SELECT to find which file to delete.
    @Query("DELETE FROM movie_media WHERE movie_id = :movieId AND kind = :kind RETURNING storage_key")
    fun deleteByMovieIdAndKind(movieId: UUID, kind: String): String?
}
