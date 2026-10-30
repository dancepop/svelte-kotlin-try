package com.backend.movie.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.InsertOnlyProperty
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

/** A row of `people`: anyone who can be credited on a movie, as cast or creator. The rows come from seed data. */
@Table("people")
data class Person(
    @Id val id: UUID? = null,
    val name: String,
    val bio: String? = null,
    @InsertOnlyProperty val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
