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
import java.util.UUID

@GrpcService
class PersonGrpcController(
    private val personService: PersonService,
) : PersonServiceGrpcKt.PersonServiceCoroutineImplBase() {

    override suspend fun listPeople(request: ListPeopleRequest): ListPeopleResponse = rpc(request) {
        val page = personService.listPeople(request.query, request.page, request.pageSize)
        listPeopleResponse {
            people += page.items.map { it.toProto() }
            totalCount = page.totalCount
        }
    }

    override suspend fun getPerson(request: GetPersonRequest): Person = rpc(request) {
        personService.getPerson(UUID.fromString(request.id)).toProto()
    }

    override suspend fun createPerson(request: CreatePersonRequest): Person = rpc(request) {
        val fields = request.fields
        personService.createPerson(fields.name, fields.bioOrNull()).toProto()
    }

    override suspend fun updatePerson(request: UpdatePersonRequest): Person = rpc(request) {
        val fields = request.fields
        personService.updatePerson(UUID.fromString(request.id), fields.name, fields.bioOrNull()).toProto()
    }

    override suspend fun deletePerson(request: DeletePersonRequest): Empty = rpc(request) {
        personService.deletePerson(UUID.fromString(request.id))
        Empty.getDefaultInstance()
    }

    // hasBio checks if client side actually sends it. protobuf defaults it as empty string ""
    private fun PersonFields.bioOrNull(): String? = if (hasBio()) bio else null
}
