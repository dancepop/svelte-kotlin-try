package com.backend.movie.controller

import com.backend.movie.api.createPersonRequest
import com.backend.movie.api.listPeopleRequest
import com.backend.movie.api.personFields
import com.backend.movie.api.updatePersonRequest
import com.backend.movie.entities.Person
import com.backend.movie.model.Page
import com.backend.movie.service.PersonService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PersonGrpcControllerTests {
    private val personService = mockk<PersonService>()
    private val controller = PersonGrpcController(personService)

    private val personId = UUID.randomUUID()

    @Test
    fun listPeoplePassesQueryAndReturnsPage() = runTest {
        every { personService.listPeople(any(), any(), any()) } returns Page(listOf(Person(id = personId, name = "Al Pacino")), 7)

        val response = controller.listPeople(listPeopleRequest { query = "pacino"; page = 1; pageSize = 5 })

        verify { personService.listPeople("pacino", 1, 5) }
        assertEquals(listOf("Al Pacino"), response.peopleList.map { it.name })
        assertEquals(7, response.totalCount)
    }

    @Test
    fun createPersonWithoutBio() = runTest {
        every { personService.createPerson("Al Pacino", null) } returns Person(id = personId, name = "Al Pacino")

        val response = controller.createPerson(createPersonRequest { fields = personFields { name = "Al Pacino" } })

        assertEquals(personId.toString(), response.id)
        assertFalse(response.hasBio())
    }

    @Test
    fun updatePersonWithBio() = runTest {
        every { personService.updatePerson(personId, "Al Pacino", "Actor") } returns Person(id = personId, name = "Al Pacino", bio = "Actor")

        val response = controller.updatePerson(
            updatePersonRequest { id = personId.toString(); fields = personFields { name = "Al Pacino"; bio = "Actor" } },
        )

        assertEquals("Actor", response.bio)
    }
}
