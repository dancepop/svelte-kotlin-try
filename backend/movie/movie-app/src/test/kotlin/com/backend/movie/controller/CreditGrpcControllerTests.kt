package com.backend.movie.controller

import com.backend.movie.api.creditRequest
import com.backend.movie.api.listCreditsByMoviesRequest
import com.backend.movie.api.listCreditsByPeopleRequest
import com.backend.movie.enums.CreditType
import com.backend.movie.model.MovieCreditRow
import com.backend.movie.model.PersonCreditRow
import com.backend.movie.service.CreditService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import com.backend.movie.api.CreditType as ProtoCreditType

class CreditGrpcControllerTests {

    private val creditService = mockk<CreditService>()
    private val controller = CreditGrpcController(creditService)

    // Not named movieId/personId, which inside the request builders would mean the request's own fields.
    private val movie = UUID.randomUUID()
    private val person = UUID.randomUUID()

    @Test
    fun addAndRemoveCreditConvertType() = runTest {
        every { creditService.addCredit(any(), any(), any()) } just runs
        every { creditService.removeCredit(any(), any(), any()) } just runs

        controller.addCredit(creditRequest { movieId = movie.toString(); personId = person.toString(); type = ProtoCreditType.CAST })
        controller.removeCredit(creditRequest { movieId = movie.toString(); personId = person.toString(); type = ProtoCreditType.CREATOR })

        verify { creditService.addCredit(movie, person, CreditType.CAST) }
        verify { creditService.removeCredit(movie, person, CreditType.CREATOR) }
    }

    @Test
    fun listCreditsByMoviesReturnsNames() = runTest {
        every { creditService.listCreditsByMovies(listOf(movie)) } returns
            listOf(MovieCreditRow(movie, person, "Michael Mann", CreditType.CREATOR))

        val response = controller.listCreditsByMovies(listCreditsByMoviesRequest { movieIds += movie.toString() })

        val credit = response.creditsList.single()
        assertEquals(person.toString(), credit.personId)
        assertEquals("Michael Mann", credit.name)
        assertEquals(ProtoCreditType.CREATOR, credit.type)
    }

    @Test
    fun listCreditsByPeopleReturnsTitles() = runTest {
        every { creditService.listCreditsByPeople(listOf(person)) } returns
            listOf(PersonCreditRow(person, movie, "Heat", CreditType.CAST))

        val response = controller.listCreditsByPeople(listCreditsByPeopleRequest { personIds += person.toString() })

        val credit = response.creditsList.single()
        assertEquals(movie.toString(), credit.movieId)
        assertEquals("Heat", credit.title)
        assertEquals(ProtoCreditType.CAST, credit.type)
    }
}
