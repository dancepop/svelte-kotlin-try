package com.backend.movie.controller

import com.backend.movie.api.CreditRequest
import com.backend.movie.api.CreditServiceGrpcKt
import com.backend.movie.api.ListCreditsByMoviesRequest
import com.backend.movie.api.ListCreditsByMoviesResponse
import com.backend.movie.api.ListCreditsByPeopleRequest
import com.backend.movie.api.ListCreditsByPeopleResponse
import com.backend.movie.api.listCreditsByMoviesResponse
import com.backend.movie.api.listCreditsByPeopleResponse
import com.backend.movie.service.CreditService
import com.google.protobuf.Empty
import org.springframework.grpc.server.service.GrpcService

/** gRPC entry point for credits. Rejects invalid values and converts between proto and Kotlin types; the business logic is in [CreditService]. */
@GrpcService
class CreditGrpcController(
    private val creditService: CreditService,
) : CreditServiceGrpcKt.CreditServiceCoroutineImplBase() {

    override suspend fun addCredit(request: CreditRequest): Empty = rpc {
        creditService.addCredit(
            parseUuid(request.movieId, "movieId"),
            parseUuid(request.personId, "personId"),
            request.type.toEnum(),
        )
        Empty.getDefaultInstance()
    }

    override suspend fun removeCredit(request: CreditRequest): Empty = rpc {
        creditService.removeCredit(
            parseUuid(request.movieId, "movieId"),
            parseUuid(request.personId, "personId"),
            request.type.toEnum(),
        )
        Empty.getDefaultInstance()
    }

    override suspend fun listCreditsByMovies(request: ListCreditsByMoviesRequest): ListCreditsByMoviesResponse = rpc {
        requireValid(idCountErrors("movieIds", request.movieIdsCount))
        val rows = creditService.listCreditsByMovies(request.movieIdsList.map { parseUuid(it, "movieIds") })
        listCreditsByMoviesResponse { credits += rows.map { it.toProto() } }
    }

    override suspend fun listCreditsByPeople(request: ListCreditsByPeopleRequest): ListCreditsByPeopleResponse = rpc {
        requireValid(idCountErrors("personIds", request.personIdsCount))
        val rows = creditService.listCreditsByPeople(request.personIdsList.map { parseUuid(it, "personIds") })
        listCreditsByPeopleResponse { credits += rows.map { it.toProto() } }
    }
}
