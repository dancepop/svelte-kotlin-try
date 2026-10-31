package com.backend.movie.service

import com.backend.movie.entities.Person
import com.backend.movie.exception.NotFoundException
import com.backend.movie.repository.PersonRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PersonServiceTests {

    private val personRepository = mockk<PersonRepository>()
    private val service = PersonService(personRepository)

    private val id = UUID.randomUUID()
    private val lastUpdate = Instant.parse("2025-01-01T00:00:00Z")

    @BeforeTest
    fun setUp() {
        every { personRepository.save(any()) } answers { firstArg() }
        every { personRepository.deleteById(any()) } just runs
    }

    private fun givenStored(person: Person?) {
        every { personRepository.findById(id) } returns Optional.ofNullable(person)
    }

    @Test
    fun listPeople() {
        every { personRepository.search(any(), any(), any(), any()) } returns emptyList()
        every { personRepository.countSearch(any()) } returns 0

        service.listPeople(" amy ", 3, 10)

        verify { personRepository.search("amy", "%amy%", 10, 30L) }
        verify { personRepository.countSearch("%amy%") }
    }

    @Test
    fun createPersonTrimsName() {
        assertEquals("Amy Adams", service.createPerson("  Amy Adams ", null).name)
    }

    @Test
    fun updatePerson() {
        givenStored(Person(id = id, name = "Amy", updatedAt = lastUpdate))

        val updated = service.updatePerson(id, "Amy Adams", "Actor")

        assertEquals("Amy Adams", updated.name)
        assertEquals("Actor", updated.bio)
        assertTrue(updated.updatedAt > lastUpdate)
    }

    @Test
    fun unknownIdIsNotFound() {
        givenStored(null)

        assertFailsWith<NotFoundException> { service.getPerson(id) }
        assertFailsWith<NotFoundException> { service.updatePerson(id, "Amy", null) }
        assertFailsWith<NotFoundException> { service.deletePerson(id) }
        verify(exactly = 0) { personRepository.save(any()) }
        verify(exactly = 0) { personRepository.deleteById(any()) }
    }

    @Test
    fun deletePerson() {
        givenStored(Person(id = id, name = "Amy"))

        service.deletePerson(id)

        verify { personRepository.deleteById(id) }
    }
}
