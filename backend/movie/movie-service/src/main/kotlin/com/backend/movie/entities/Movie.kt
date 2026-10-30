package com.backend.movie.entities

import com.backend.movie.enums.MovieStatus
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.InsertOnlyProperty
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * A row of `movies`. Columns map by name: `releaseDate` → `release_date`.
 *
 * `id` and `version` are null until the first save: null tells Spring Data JDBC to INSERT,
 * leave `id` out so Postgres fills it with `uuidv7()`, and start `version` at 0.
 */
@Table("movies")
data class Movie(
    @Id val id: UUID? = null,
    val title: String,
    val synopsis: String? = null,
    val releaseDate: LocalDate? = null,
    val runtimeMinutes: Int? = null,
    val status: MovieStatus = MovieStatus.DRAFT,
    // Written on INSERT only, so an update can never change it.
    @InsertOnlyProperty val createdAt: Instant = Instant.now(),
    // The service sets a new value on every update.
    val updatedAt: Instant = Instant.now(),
    // UPDATE … WHERE version = ?; 0 rows updated means someone else saved first.
    @Version val version: Int? = null,
)
