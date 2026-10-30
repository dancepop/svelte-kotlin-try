package com.backend.movie.service

import com.backend.movie.enums.CreditType
import com.backend.movie.exception.AlreadyExistsException
import com.backend.movie.exception.NotFoundException
import com.backend.movie.model.MovieCreditRow
import com.backend.movie.model.PersonCreditRow
import com.backend.movie.repository.CreditRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CreditService(private val creditRepository: CreditRepository) {

    @Transactional
    fun addCredit(movieId: UUID, personId: UUID, type: CreditType) {
        try {
            creditRepository.insert(movieId, personId, type.name)
        } catch (e: DataIntegrityViolationException) {
            // The constraint name says which rule was broken.
            val message = e.mostSpecificCause.message.orEmpty()
            when {
                "pk_credits" in message -> throw AlreadyExistsException(duplicateMessage(type))
                "fk_credits_movie_id" in message -> throw NotFoundException("This movie no longer exists")
                "fk_credits_person_id" in message -> throw NotFoundException("This person no longer exists")
                else -> throw e
            }
        }
    }

    /** Removing a credit that's already gone succeeds, so a double click shows no error. */
    @Transactional
    fun removeCredit(movieId: UUID, personId: UUID, type: CreditType) {
        creditRepository.delete(movieId, personId, type.name)
    }

    /** One query for many movies, so the gateway's loader can fetch a whole page's credits in one call. */
    @Transactional(readOnly = true)
    fun listCreditsByMovies(movieIds: List<UUID>): List<MovieCreditRow> {
        // An empty list would produce invalid SQL: IN ()
        if (movieIds.isEmpty()) return emptyList()
        return creditRepository.findByMovieIds(movieIds)
    }

    /** One query for many people, so the gateway's loader can fetch a whole page's credits in one call. */
    @Transactional(readOnly = true)
    fun listCreditsByPeople(personIds: List<UUID>): List<PersonCreditRow> {
        if (personIds.isEmpty()) return emptyList()
        return creditRepository.findByPersonIds(personIds)
    }

    private fun duplicateMessage(type: CreditType): String =
        "Already credited as ${type.name.lowercase()} on this movie"
}
