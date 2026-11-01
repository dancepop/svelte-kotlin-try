package com.backend.movie.model

import com.backend.movie.enums.CreditType
import java.util.UUID

data class MovieCreditRow(
    val movieId: UUID,
    val personId: UUID,
    val name: String,
    val creditType: CreditType,
)

data class PersonCreditRow(
    val personId: UUID,
    val movieId: UUID,
    val title: String,
    val creditType: CreditType,
)
