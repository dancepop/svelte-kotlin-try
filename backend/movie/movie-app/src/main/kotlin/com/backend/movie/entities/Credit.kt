package com.backend.movie.entities

import com.backend.movie.enums.CreditType
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("credits")
data class Credit(
    // @Embedded for composite key. id never null so .empty doesn't matter but required apparently.
    // Private so Kotlin doesn't generate a getId() that clashes with Persistable's.
    @Id @Embedded.Empty private val id: CreditId,
    @CreatedDate val createdAt: Instant = Instant.now(),
) : Persistable<CreditId> {
    override fun getId(): CreditId = id
    // Always inserts since ID is always present
    override fun isNew(): Boolean = true
}

// Composite key
data class CreditId(
    val movieId: UUID,
    val personId: UUID,
    val creditType: CreditType,
)
