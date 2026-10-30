package com.backend.movie.repository

import com.backend.movie.entities.Movie
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

// Shared by the page and the count, so totalCount always matches the filters.
// Each pattern is the escaped ILIKE pattern, or empty for "no filter". EXISTS only asks whether
// a matching credit exists, so a movie crediting two Davids still appears once.
private const val MOVIE_FILTER = """
    WHERE (:titlePattern = '' OR m.title ILIKE :titlePattern)
      AND (:castPattern = '' OR EXISTS (
            SELECT 1 FROM credits c JOIN people p ON p.id = c.person_id
            WHERE c.movie_id = m.id AND c.credit_type = 'CAST' AND p.name ILIKE :castPattern))
      AND (:creatorPattern = '' OR EXISTS (
            SELECT 1 FROM credits c JOIN people p ON p.id = c.person_id
            WHERE c.movie_id = m.id AND c.credit_type = 'CREATOR' AND p.name ILIKE :creatorPattern))
"""

interface MovieRepository : CrudRepository<Movie, UUID> {

    // :term is the plain title text, for ranking. id comes last because titles can repeat:
    // without it, those rows could swap places between pages.
    @Query(
        """
        SELECT m.* FROM movies m
        $MOVIE_FILTER
        ORDER BY CASE WHEN length(:term) >= 3 THEN similarity(m.title, :term) END DESC NULLS LAST, m.title, m.id
        LIMIT :limit OFFSET :offset
        """,
    )
    fun search(
        term: String,
        titlePattern: String,
        castPattern: String,
        creatorPattern: String,
        limit: Int,
        offset: Long,
    ): List<Movie>

    @Query("SELECT count(*) FROM movies m $MOVIE_FILTER")
    fun countSearch(titlePattern: String, castPattern: String, creatorPattern: String): Long
}
