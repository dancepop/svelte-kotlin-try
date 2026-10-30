package com.backend.movie.model

import com.backend.movie.entities.Movie
import com.backend.movie.entities.MovieMedia

/** A movie as the RPCs return it. Credits aren't included: they're fetched separately, only when asked for. */
data class MovieWithMedia(val movie: Movie, val media: List<MovieMedia>)
