package com.backend.movie.repository

import com.backend.movie.entities.Person
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface PersonRepository : CrudRepository<Person, UUID> {

    // :pattern is the escaped ILIKE pattern, or empty for "no filter". :term is the plain text, for ranking.
    // id comes last because names can repeat: without it, those rows could swap places between pages.
    @Query(
        """
        SELECT p.* FROM people p
        WHERE :pattern = '' OR p.name ILIKE :pattern
        ORDER BY CASE WHEN length(:term) >= 3 THEN similarity(p.name, :term) END DESC NULLS LAST, p.name, p.id
        LIMIT :limit OFFSET :offset
        """,
    )
    fun search(term: String, pattern: String, limit: Int, offset: Long): List<Person>

    @Query("SELECT count(*) FROM people p WHERE :pattern = '' OR p.name ILIKE :pattern")
    fun countSearch(pattern: String): Long
}
