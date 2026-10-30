package com.backend.movie.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.InsertOnlyProperty
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * A row of `movies`. Columns map by name: `releaseDate` → `release_date`.
 *
 * `id` is null until the first save: null tells Spring Data JDBC to INSERT and leave `id` out,
 * so Postgres fills it with `uuidv7()`.
 */
@Table("movies")
data class Movie(
    @Id val id: UUID? = null,
    val title: String,
    val synopsis: String? = null,
    val releaseDate: LocalDate? = null,
    val runtimeMinutes: Int? = null,
    // Path under media-service's media folder, e.g. "{movieId}/{uuid}.jpg".
    val artworkKey: String? = null,
    // Written on INSERT only, so an update can never change it.
    @InsertOnlyProperty val createdAt: Instant = Instant.now(),
    // The service sets a new value on every update.
    val updatedAt: Instant = Instant.now(),
)
