package com.backend.movie.service

import com.backend.movie.entities.Credit
import com.backend.movie.entities.CreditId
import com.backend.movie.enums.CreditType
import com.backend.movie.exception.AlreadyExistsException
import com.backend.movie.exception.NotFoundException
import com.backend.movie.model.MovieCreditRow
import com.backend.movie.model.PersonCreditRow
import com.backend.movie.repository.CreditRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CreditService(private val creditRepository: CreditRepository) {

    fun addCredit(movieId: UUID, personId: UUID, type: CreditType) {
        try {
            creditRepository.save(Credit(CreditId(movieId, personId, type)))
        } catch (e: DataIntegrityViolationException) {
            val message = e.mostSpecificCause.message.orEmpty()
            when {
                "pk_credits" in message -> throw AlreadyExistsException("Already credited as ${type.name.lowercase()} on this movie")
                "fk_credits_movie_id" in message -> throw NotFoundException("This movie no longer exists")
                "fk_credits_person_id" in message -> throw NotFoundException("This person no longer exists")
                else -> throw e
            }
        }
    }

    fun removeCredit(movieId: UUID, personId: UUID, type: CreditType) {
        creditRepository.deleteById(CreditId(movieId, personId, type))
    }

    // Used by gateway's dataloader can bulk fetch movie credits in one call
    // Currently not used with more than 1 id in func argument but wanted
    // to try out graphql N+1 mitigation for completion.
    fun listCreditsByMovies(movieIds: List<UUID>): List<MovieCreditRow> {
        if (movieIds.isEmpty()) return emptyList()
        return creditRepository.findByMovieIds(movieIds)
    }

    // Used so the gateway's dataloader can bulk fetch credits in one call
    // Currently not used with more than 1 id in func argument but wanted
    // to try out graphql N+1 mitigation for completion.
    fun listCreditsByPeople(personIds: List<UUID>): List<PersonCreditRow> {
        if (personIds.isEmpty()) return emptyList()
        return creditRepository.findByPersonIds(personIds)
    }
}
