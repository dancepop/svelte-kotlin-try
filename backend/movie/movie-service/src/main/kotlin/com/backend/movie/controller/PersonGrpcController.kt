package com.backend.movie.controller

import com.backend.movie.api.GetPersonRequest
import com.backend.movie.api.ListPeopleRequest
import com.backend.movie.api.ListPeopleResponse
import com.backend.movie.api.Person
import com.backend.movie.api.PersonServiceGrpcKt
import com.backend.movie.api.listPeopleResponse
import com.backend.movie.service.PersonService
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
}
