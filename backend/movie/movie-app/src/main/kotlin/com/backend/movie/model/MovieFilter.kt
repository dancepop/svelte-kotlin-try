package com.backend.movie.model

data class MovieFilter(
    val query: String = "",
    val castName: String = "",
    val creatorName: String = "",
    val page: Int = 0,
    val pageSize: Int = 20,
)
