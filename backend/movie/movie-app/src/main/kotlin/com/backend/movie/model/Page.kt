package com.backend.movie.model

/** One page of results, and how many exist in total across all pages. */
data class Page<T>(val items: List<T>, val totalCount: Long)
