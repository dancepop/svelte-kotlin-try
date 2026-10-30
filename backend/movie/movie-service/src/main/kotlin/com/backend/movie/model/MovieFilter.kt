package com.backend.movie.model

/** Search input for the movie list. Every value given must match; an empty one is skipped. */
data class MovieFilter(
    val query: String = "",
    val castName: String = "",
    val creatorName: String = "",
    val page: Int = 0,
    val pageSize: Int = 20,
)
