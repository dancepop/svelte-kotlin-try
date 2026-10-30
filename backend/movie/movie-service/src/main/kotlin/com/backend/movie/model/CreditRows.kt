package com.backend.movie.model

import com.backend.movie.enums.CreditType
import com.backend.movie.enums.MovieStatus
import java.util.UUID

/** A credit joined with its person: a movie's cast and creators. */
data class MovieCreditRow(
    val movieId: UUID,
    val personId: UUID,
    val name: String,
    val creditType: CreditType,
)

/** A credit joined with its movie: the movies a person worked on. */
data class PersonCreditRow(
    val personId: UUID,
    val movieId: UUID,
    val title: String,
    val status: MovieStatus,
    val creditType: CreditType,
)
