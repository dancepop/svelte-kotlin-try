package com.backend.movie.controller

import com.backend.movie.api.v1.CreditRequest
import com.backend.movie.api.v1.CreditServiceGrpcKt
import com.backend.movie.api.v1.ListCreditsByMoviesRequest
import com.backend.movie.api.v1.ListCreditsByMoviesResponse
import com.backend.movie.api.v1.ListCreditsByPeopleRequest
import com.backend.movie.api.v1.ListCreditsByPeopleResponse
import com.backend.movie.api.v1.listCreditsByMoviesResponse
import com.backend.movie.api.v1.listCreditsByPeopleResponse
import com.backend.movie.service.CreditService
import com.google.protobuf.Empty
import org.springframework.grpc.server.service.GrpcService

/** gRPC entry point for credits. Only converts between proto and Kotlin types; the rules are in [CreditService]. */
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
        val rows = creditService.listCreditsByMovies(request.movieIdsList.map { parseUuid(it, "movieIds") })
        listCreditsByMoviesResponse { credits += rows.map { it.toProto() } }
    }

    override suspend fun listCreditsByPeople(request: ListCreditsByPeopleRequest): ListCreditsByPeopleResponse = rpc {
        val rows = creditService.listCreditsByPeople(request.personIdsList.map { parseUuid(it, "personIds") })
        listCreditsByPeopleResponse { credits += rows.map { it.toProto() } }
    }
}
