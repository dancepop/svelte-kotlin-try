package com.backend.movie.repository

import com.backend.movie.entities.Credit
import com.backend.movie.entities.CreditId
import com.backend.movie.model.MovieCreditRow
import com.backend.movie.model.PersonCreditRow
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface CreditRepository : CrudRepository<Credit, CreditId> {

    @Query(
        """
        SELECT c.movie_id, c.person_id, p.name, c.credit_type
        FROM credits c JOIN people p ON p.id = c.person_id
        WHERE c.movie_id IN (:movieIds)
        ORDER BY p.name, p.id
        """,
    )
    fun findByMovieIds(movieIds: Collection<UUID>): List<MovieCreditRow>

    @Query(
        """
        SELECT c.person_id, c.movie_id, m.title, c.credit_type
        FROM credits c JOIN movies m ON m.id = c.movie_id
        WHERE c.person_id IN (:personIds)
        ORDER BY m.title, m.id
        """,
    )
    fun findByPersonIds(personIds: Collection<UUID>): List<PersonCreditRow>
}
