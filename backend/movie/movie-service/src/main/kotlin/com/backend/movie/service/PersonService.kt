package com.backend.movie.service

import com.backend.movie.entities.Person
import com.backend.movie.exception.NotFoundException
import com.backend.movie.model.Page
import com.backend.movie.repository.PersonRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class PersonService(private val personRepository: PersonRepository) {

    @Transactional(readOnly = true)
    fun listPeople(query: String, page: Int, pageSize: Int): Page<Person> {
        val pattern = likePattern(query)
        val people = personRepository.search(query.trim(), pattern, pageSize, offsetOf(page, pageSize))
        return Page(people, personRepository.countSearch(pattern))
    }

    @Transactional(readOnly = true)
    fun getPerson(id: UUID): Person = findPerson(id)

    @Transactional
    fun createPerson(name: String, bio: String?): Person =
        personRepository.save(Person(name = name.trim(), bio = bio))

    @Transactional
    fun updatePerson(id: UUID, name: String, bio: String?): Person {
        val changed = findPerson(id).copy(name = name.trim(), bio = bio, updatedAt = Instant.now())
        return personRepository.save(changed)
    }

    @Transactional
    fun deletePerson(id: UUID) {
        findPerson(id)
        // credits go with it through the cascade.
        personRepository.deleteById(id)
    }

    private fun findPerson(id: UUID): Person =
        personRepository.findByIdOrNull(id) ?: throw NotFoundException("This person no longer exists")
}
