package com.backend.movie.model

import com.backend.movie.enums.MovieStatus

/** Search input for the movie list. Every value given must match; an empty one is skipped. */
data class MovieFilter(
    val query: String = "",
    val castName: String = "",
    val creatorName: String = "",
    val status: MovieStatus = MovieStatus.PUBLISHED,
    val page: Int = 0,
    val pageSize: Int = 20,
)
