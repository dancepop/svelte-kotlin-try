package com.backend.media.controller

import com.backend.media.exception.MovieServiceUnavailableException
import com.backend.media.exception.NotFoundException
import build.buf.protovalidate.ValidatorFactory
import build.buf.validate.FieldPath
import com.backend.media.exception.ValidationException
import com.google.protobuf.Any
import com.google.protobuf.Message
import com.google.rpc.BadRequest
import com.google.rpc.Code
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.protobuf.StatusProto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.backend.media.controller.GrpcExceptionHandler")

// Reads the (buf.validate.field) rules from each message's descriptor the first time it sees that message type.
private val validator = ValidatorFactory.newBuilder().build()

/**
 * Wraps controller methods. Runs blocking work on the coroutine pool meant for it, and turns exceptions into
 * gRPC statuses. Spring's exception handling can't do this here: grpc-kotlin catches exceptions inside
 * the coroutine, so they'd reach the client as UNKNOWN.
 */
internal suspend fun <T> rpc(block: suspend () -> T): T = try {
    withContext(Dispatchers.IO) { block() }
} catch (e: CancellationException) {
    // The client went away or its deadline passed. This must stay a cancellation.
    throw e
} catch (e: Exception) {
    throw toStatusException(e)
}

/** Each media message has at most one rule, so the first broken one is the whole story. */
internal fun requireValid(request: Message) {
    val violation = validator.validate(request).violations.firstOrNull() ?: return
    val proto = violation.toProto()
    throw ValidationException(fieldNameOf(proto.field), proto.message)
}

// The path ends at the broken field (movie_id). Sent in camelCase (movieId), like movie-service does.
private fun fieldNameOf(path: FieldPath): String {
    val name = path.elementsList.lastOrNull()?.fieldName.orEmpty()
    return name.split('_').mapIndexed { i, part -> if (i == 0) part else part.replaceFirstChar(Char::uppercase) }.joinToString("")
}

internal fun toStatusException(e: Throwable): StatusException = when (e) {
    is ValidationException -> invalidArgument(e.field, e.message.orEmpty())
    is NotFoundException -> StatusException(Status.NOT_FOUND.withDescription(e.message))
    is MovieServiceUnavailableException -> {
        logger.warn("Couldn't reach movie-service", e)
        StatusException(Status.UNAVAILABLE.withDescription("Something went wrong, try again"))
    }
    else -> {
        // Details stay in the logs; the caller only learns that it failed.
        logger.error("Unexpected error in a gRPC call", e)
        StatusException(Status.INTERNAL.withDescription("Something went wrong"))
    }
}

/**
 * Google's standard error model, the same as movie-service sends: a google.rpc.Status holding a BadRequest.
 * gRPC sends it in the grpc-status-details-bin trailer, where the gateway reads it back.
 */
private fun invalidArgument(field: String, message: String): StatusException {
    val violation = BadRequest.FieldViolation.newBuilder().setField(field).setDescription(message).build()
    val status = com.google.rpc.Status.newBuilder()
        .setCode(Code.INVALID_ARGUMENT_VALUE)
        .setMessage(message)
        .addDetails(Any.pack(BadRequest.newBuilder().addFieldViolations(violation).build()))
        .build()
    return StatusProto.toStatusException(status)
}
