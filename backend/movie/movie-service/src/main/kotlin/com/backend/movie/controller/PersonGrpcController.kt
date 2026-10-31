package com.backend.movie.controller

import com.backend.movie.api.CreatePersonRequest
import com.backend.movie.api.DeletePersonRequest
import com.backend.movie.api.GetPersonRequest
import com.backend.movie.api.ListPeopleRequest
import com.backend.movie.api.ListPeopleResponse
import com.backend.movie.api.Person
import com.backend.movie.api.PersonFields
import com.backend.movie.api.PersonServiceGrpcKt
import com.backend.movie.api.UpdatePersonRequest
import com.backend.movie.api.listPeopleResponse
import com.backend.movie.service.PersonService
import com.google.protobuf.Empty
import org.springframework.grpc.server.service.GrpcService

/** gRPC entry point for people. Rejects invalid values and converts between proto and Kotlin types; the business logic is in [PersonService]. */
@GrpcService
class PersonGrpcController(
    private val personService: PersonService,
) : PersonServiceGrpcKt.PersonServiceCoroutineImplBase() {

    override suspend fun listPeople(request: ListPeopleRequest): ListPeopleResponse = rpc {
        requireValid(pagingErrors(request.page, request.pageSize))
        val page = personService.listPeople(request.query, request.page, request.pageSize)
        listPeopleResponse {
            people += page.items.map { it.toProto() }
            totalCount = page.totalCount
        }
    }

    override suspend fun getPerson(request: GetPersonRequest): Person = rpc {
        personService.getPerson(parseUuid(request.id, "id")).toProto()
    }

    override suspend fun createPerson(request: CreatePersonRequest): Person = rpc {
        val fields = request.fields
        fields.requireValidFields()
        personService.createPerson(fields.name, fields.bioOrNull()).toProto()
    }

    override suspend fun updatePerson(request: UpdatePersonRequest): Person = rpc {
        val fields = request.fields
        fields.requireValidFields()
        personService.updatePerson(parseUuid(request.id, "id"), fields.name, fields.bioOrNull()).toProto()
    }

    override suspend fun deletePerson(request: DeletePersonRequest): Empty = rpc {
        personService.deletePerson(parseUuid(request.id, "id"))
        Empty.getDefaultInstance()
    }

    private fun PersonFields.requireValidFields() {
        requireValid(personFieldErrors(name, bioOrNull()))
    }

    private fun PersonFields.bioOrNull(): String? = if (hasBio()) bio else null
}
