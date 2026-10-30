package com.backend.movie.entities

import com.backend.movie.enums.CreditType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

/**
 * A row of `credits`: one person's part in one movie. The whole primary key is [CreditId].
 *
 * The id is never null, so Spring Data JDBC can't tell a new credit from an existing one and
 * `save()` would run an UPDATE. Credits are only ever added or removed, so the repository inserts them explicitly.
 */
@Table("credits")
data class Credit(
    // @Embedded: CreditId's fields become this table's columns (movie_id, person_id, credit_type).
    @Id @Embedded.Empty val id: CreditId,
    val createdAt: Instant = Instant.now(),
)

data class CreditId(
    val movieId: UUID,
    val personId: UUID,
    val creditType: CreditType,
)
