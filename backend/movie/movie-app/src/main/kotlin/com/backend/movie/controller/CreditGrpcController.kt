package com.backend.movie.controller

import com.backend.movie.api.CreditRequest
import com.backend.movie.api.CreditServiceGrpcKt
import com.backend.movie.api.ListCreditsByMoviesRequest
import com.backend.movie.api.ListCreditsByMoviesResponse
import com.backend.movie.api.ListCreditsByPeopleRequest
import com.backend.movie.api.ListCreditsByPeopleResponse
import com.backend.movie.api.listCreditsByMoviesResponse
import com.backend.movie.api.listCreditsByPeopleResponse
import com.backend.movie.enums.CreditType
import com.backend.movie.service.CreditService
import com.google.protobuf.Empty
import org.springframework.grpc.server.service.GrpcService
import java.util.UUID

@GrpcService
class CreditGrpcController(
    private val creditService: CreditService,
) : CreditServiceGrpcKt.CreditServiceCoroutineImplBase() {

    override suspend fun addCredit(request: CreditRequest): Empty = rpc(request) {
        creditService.addCredit(
            UUID.fromString(request.movieId),
            UUID.fromString(request.personId),
            CreditType.valueOf(request.type.name),
        )
        Empty.getDefaultInstance()
    }

    override suspend fun removeCredit(request: CreditRequest): Empty = rpc(request) {
        creditService.removeCredit(
            UUID.fromString(request.movieId),
            UUID.fromString(request.personId),
            CreditType.valueOf(request.type.name),
        )
        Empty.getDefaultInstance()
    }

    override suspend fun listCreditsByMovies(request: ListCreditsByMoviesRequest): ListCreditsByMoviesResponse = rpc(request) {
        val rows = creditService.listCreditsByMovies(request.movieIdsList.map { UUID.fromString(it) })
        listCreditsByMoviesResponse { credits += rows.map { it.toProto() } }
    }

    override suspend fun listCreditsByPeople(request: ListCreditsByPeopleRequest): ListCreditsByPeopleResponse = rpc(request) {
        val rows = creditService.listCreditsByPeople(request.personIdsList.map { UUID.fromString(it) })
        listCreditsByPeopleResponse { credits += rows.map { it.toProto() } }
    }
}
