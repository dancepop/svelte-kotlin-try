package com.backend.movie.service

import com.backend.movie.enums.CreditType.CAST
import com.backend.movie.enums.CreditType.CREATOR
import com.backend.movie.exception.AlreadyExistsException
import com.backend.movie.exception.NotFoundException
import com.backend.movie.repository.CreditRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.dao.DataIntegrityViolationException
import java.sql.SQLException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CreditServiceTests {

    private val creditRepository = mockk<CreditRepository>()
    private val service = CreditService(creditRepository)

    private val movieId = UUID.randomUUID()
    private val personId = UUID.randomUUID()

    // What Spring throws when Postgres rejects the INSERT; CreditRepositoryTest checks the real one names the constraint.
    private fun givenInsertFails(postgresMessage: String): DataIntegrityViolationException {
        val error = DataIntegrityViolationException("INSERT failed", SQLException(postgresMessage))
        every { creditRepository.insert(any(), any(), any()) } throws error
        return error
    }

    @Test
    fun addAndRemoveCredit() {
        every { creditRepository.insert(any(), any(), any()) } returns 1
        every { creditRepository.delete(any(), any(), any()) } returns 1

        service.addCredit(movieId, personId, CAST)
        service.removeCredit(movieId, personId, CREATOR)

        verify { creditRepository.insert(movieId, personId, "CAST") }
        verify { creditRepository.delete(movieId, personId, "CREATOR") }
    }

    @Test
    fun addDuplicateCreditIsAlreadyExists() {
        givenInsertFails("""duplicate key value violates unique constraint "pk_credits"""")

        val error = assertFailsWith<AlreadyExistsException> { service.addCredit(movieId, personId, CREATOR) }

        assertEquals("Already credited as creator on this movie", error.message)
    }

    @Test
    fun addCreditForUnknownMovieIsNotFound() {
        givenInsertFails("""violates foreign key constraint "fk_credits_movie_id"""")

        val error = assertFailsWith<NotFoundException> { service.addCredit(movieId, personId, CAST) }

        assertEquals("This movie no longer exists", error.message)
    }

    @Test
    fun addCreditForUnknownPersonIsNotFound() {
        givenInsertFails("""violates foreign key constraint "fk_credits_person_id"""")

        val error = assertFailsWith<NotFoundException> { service.addCredit(movieId, personId, CAST) }

        assertEquals("This person no longer exists", error.message)
    }

    @Test
    fun addCreditPassesOnOtherErrors() {
        val original = givenInsertFails("""violates some other constraint""")

        val error = assertFailsWith<DataIntegrityViolationException> { service.addCredit(movieId, personId, CAST) }

        assertSame(original, error)
    }

    @Test
    fun listCreditsWithNoIds() {
        assertTrue(service.listCreditsByMovies(emptyList()).isEmpty())
        assertTrue(service.listCreditsByPeople(emptyList()).isEmpty())

        verify(exactly = 0) { creditRepository.findByMovieIds(any()) }
        verify(exactly = 0) { creditRepository.findByPersonIds(any()) }
    }
}
