package com.backend.movie.controller

import com.backend.movie.api.v1.CreatePersonRequest
import com.backend.movie.api.v1.DeletePersonRequest
import com.backend.movie.api.v1.GetPersonRequest
import com.backend.movie.api.v1.ListPeopleRequest
import com.backend.movie.api.v1.ListPeopleResponse
import com.backend.movie.api.v1.Person
import com.backend.movie.api.v1.PersonServiceGrpcKt
import com.backend.movie.api.v1.UpdatePersonRequest
import com.backend.movie.api.v1.listPeopleResponse
import com.backend.movie.service.PersonService
import com.google.protobuf.Empty
import org.springframework.grpc.server.service.GrpcService

/** gRPC entry point for people. Only converts between proto and Kotlin types; the rules are in [PersonService]. */
@GrpcService
class PersonGrpcController(
    private val personService: PersonService,
) : PersonServiceGrpcKt.PersonServiceCoroutineImplBase() {

    override suspend fun listPeople(request: ListPeopleRequest): ListPeopleResponse = rpc {
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
        personService.createPerson(request.name, if (request.hasBio()) request.bio else null).toProto()
    }

    override suspend fun updatePerson(request: UpdatePersonRequest): Person = rpc {
        personService.updatePerson(
            parseUuid(request.id, "id"),
            request.version,
            request.name,
            if (request.hasBio()) request.bio else null,
        ).toProto()
    }

    override suspend fun deletePerson(request: DeletePersonRequest): Empty = rpc {
        personService.deletePerson(parseUuid(request.id, "id"), request.version)
        Empty.getDefaultInstance()
    }
}
