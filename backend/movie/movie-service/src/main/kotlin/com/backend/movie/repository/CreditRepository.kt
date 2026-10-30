package com.backend.movie.repository

import com.backend.movie.entities.Credit
import com.backend.movie.entities.CreditId
import com.backend.movie.model.MovieCreditRow
import com.backend.movie.model.PersonCreditRow
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.Repository
import java.util.UUID

interface CreditRepository : Repository<Credit, CreditId> {

    // Written by hand because the id is never null, so save() would run an UPDATE.
    @Modifying
    @Query("INSERT INTO credits (movie_id, person_id, credit_type) VALUES (:movieId, :personId, :creditType)")
    fun insert(movieId: UUID, personId: UUID, creditType: String): Int

    @Modifying
    @Query("DELETE FROM credits WHERE movie_id = :movieId AND person_id = :personId AND credit_type = :creditType")
    fun delete(movieId: UUID, personId: UUID, creditType: String): Int

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
        SELECT c.person_id, c.movie_id, m.title, m.status, c.credit_type
        FROM credits c JOIN movies m ON m.id = c.movie_id
        WHERE c.person_id IN (:personIds)
        ORDER BY m.title, m.id
        """,
    )
    fun findByPersonIds(personIds: Collection<UUID>): List<PersonCreditRow>
}
