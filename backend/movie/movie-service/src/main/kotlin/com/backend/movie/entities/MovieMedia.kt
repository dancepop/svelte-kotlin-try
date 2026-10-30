package com.backend.movie.entities

import com.backend.movie.enums.MediaKind
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

/**
 * A row of `movie_media`: the record of one file stored by media-service. Primary key [MovieMediaId],
 * so a movie has at most one file per [MediaKind]; a new upload replaces the row (an upsert in the repository).
 */
@Table("movie_media")
data class MovieMedia(
    @Id @Embedded.Empty val id: MovieMediaId,
    // Path under media-service's media folder, e.g. "{movieId}/artwork/{uuid}.jpg".
    val storageKey: String,
    val contentType: String,
    val sizeBytes: Long,
    // Videos only; null for ARTWORK.
    val durationSeconds: Int? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

data class MovieMediaId(
    val movieId: UUID,
    val kind: MediaKind,
)
