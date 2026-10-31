package com.backend.movie.controller

import com.google.rpc.BadRequest
import io.grpc.StatusException
import io.grpc.protobuf.StatusProto

/** The field violations an INVALID_ARGUMENT status carries, as field → message, the way the gateway reads them. */
fun fieldViolationsOf(error: StatusException): Map<String, String> {
    val status = requireNotNull(StatusProto.fromThrowable(error)) { "The status has no details" }
    return status.detailsList
        .single()
        .unpack(BadRequest::class.java)
        .fieldViolationsList
        .associate { it.field to it.description }
}
