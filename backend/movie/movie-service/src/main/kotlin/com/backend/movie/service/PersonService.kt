package com.backend.movie.service

import com.backend.movie.entities.Person
import com.backend.movie.exception.ConflictException
import com.backend.movie.exception.NotFoundException
import com.backend.movie.model.Page
import com.backend.movie.repository.PersonRepository
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private const val STALE_PERSON = "Someone else changed this person. Reload to see their changes."

@Service
class PersonService(private val personRepository: PersonRepository) {

    @Transactional(readOnly = true)
    fun listPeople(query: String, page: Int, pageSize: Int): Page<Person> {
        requireValid(pagingErrors(page, pageSize))
        val pattern = likePattern(query)
        val people = personRepository.search(query.trim(), pattern, pageSize, offsetOf(page, pageSize))
        return Page(people, personRepository.countSearch(pattern))
    }

    @Transactional(readOnly = true)
    fun getPerson(id: UUID): Person = findPerson(id)

    @Transactional
    fun createPerson(name: String, bio: String?): Person {
        requireValid(personFieldErrors(name, bio))
        return personRepository.save(Person(name = name.trim(), bio = bio))
    }

    @Transactional
    fun updatePerson(id: UUID, version: Int, name: String, bio: String?): Person {
        requireValid(personFieldErrors(name, bio))
        val person = findPerson(id)
        // Saving the version the client sent makes Spring Data run UPDATE … WHERE version = ?.
        val changed = person.copy(name = name.trim(), bio = bio, updatedAt = Instant.now(), version = version)
        try {
            return personRepository.save(changed)
        } catch (_: OptimisticLockingFailureException) {
            throw ConflictException(STALE_PERSON)
        }
    }

    /** Allowed even if they're credited on a published movie: the cascade removes those credits too. */
    @Transactional
    fun deletePerson(id: UUID, version: Int) {
        val person = findPerson(id)
        try {
            personRepository.delete(person.copy(version = version))
        } catch (_: OptimisticLockingFailureException) {
            throw ConflictException(STALE_PERSON)
        }
    }

    private fun findPerson(id: UUID): Person =
        personRepository.findByIdOrNull(id) ?: throw NotFoundException("This person no longer exists")
}
