package com.backend.movie.controller

import build.buf.protovalidate.ValidatorFactory
import build.buf.protovalidate.Violation
import build.buf.validate.FieldPath
import com.backend.movie.exception.AlreadyExistsException
import com.backend.movie.exception.FieldError
import com.backend.movie.exception.NotFoundException
import com.backend.movie.exception.ValidationException
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

private val logger = LoggerFactory.getLogger("com.backend.movie.controller.GrpcExceptionHandler")

// Reads the (buf.validate.field) rules from each message's descriptor the first time it sees that message type.
private val validator = ValidatorFactory.newBuilder().build()

/**
 * Wraps every controller method. Checks [request] against the rules in its .proto, runs blocking work on the
 * coroutine pool meant for it, and turns exceptions into gRPC statuses. Spring's exception handling can't do this
 * here: grpc-kotlin catches exceptions inside the coroutine, so they'd reach the client as UNKNOWN.
 */
internal suspend fun <T> rpc(request: Message, block: () -> T): T = try {
    requireValid(request)
    withContext(Dispatchers.IO) { block() }
} catch (e: CancellationException) {
    // The client went away or its deadline passed. This must stay a cancellation.
    throw e
} catch (e: Exception) {
    throw toStatusException(e)
}

private fun requireValid(request: Message) {
    val violations = validator.validate(request).violations
    if (violations.isNotEmpty()) throw ValidationException(violations.map { it.toFieldError() })
}

private fun Violation.toFieldError(): FieldError {
    val violation = toProto()
    return FieldError(fieldNameOf(violation.field), violation.message)
}

// The path ends at the broken field (fields.release_date). The gateway and the forms use its camelCase name (releaseDate).
private fun fieldNameOf(path: FieldPath): String {
    val name = path.elementsList.lastOrNull()?.fieldName.orEmpty()
    return name.split('_').mapIndexed { i, part -> if (i == 0) part else part.replaceFirstChar(Char::uppercase) }.joinToString("")
}

internal fun toStatusException(e: Exception): StatusException = when (e) {
    is ValidationException -> invalidArgument(e.message.orEmpty(), e.fieldErrors)
    is NotFoundException -> StatusException(Status.NOT_FOUND.withDescription(e.message))
    is AlreadyExistsException -> StatusException(Status.ALREADY_EXISTS.withDescription(e.message))
    else -> {
        // Details stay in the logs; the caller only learns that it failed.
        logger.error("Unexpected error in a gRPC call", e)
        StatusException(Status.INTERNAL.withDescription("Something went wrong"))
    }
}

/**
 * Google's standard error model: a google.rpc.Status holding a BadRequest with one entry per broken rule.
 * gRPC sends it in the grpc-status-details-bin trailer, where the gateway reads it back.
 */
private fun invalidArgument(message: String, fieldErrors: List<FieldError>): StatusException {
    val badRequest = BadRequest.newBuilder()
        .addAllFieldViolations(
            fieldErrors.map { BadRequest.FieldViolation.newBuilder().setField(it.field).setDescription(it.message).build() },
        )
        .build()
    val status = com.google.rpc.Status.newBuilder()
        .setCode(Code.INVALID_ARGUMENT_VALUE)
        .setMessage(message)
        .addDetails(Any.pack(badRequest))
        .build()
    return StatusProto.toStatusException(status)
}
