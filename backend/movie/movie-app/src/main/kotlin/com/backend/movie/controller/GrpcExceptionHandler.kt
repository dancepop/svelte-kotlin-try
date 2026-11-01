package com.backend.movie.controller

import build.buf.protovalidate.ValidatorFactory
import com.backend.movie.exception.AlreadyExistsException
import com.backend.movie.exception.NotFoundException
import com.backend.movie.exception.ValidationException
import com.google.protobuf.Message
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException

private val logger = LoggerFactory.getLogger("com.backend.movie.controller.GrpcExceptionHandler")
private val validator = ValidatorFactory.newBuilder().build()

/**
 * Wraps every controller method. Checks [request] against the rules in its .proto, runs blocking work on the
 * coroutine pool meant for it, and turns exceptions into gRPC statuses.
 */
internal suspend fun <T> rpc(request: Message, block: () -> T): T = try {
    requireValid(request)
    // Run all through IO thread since no heavy CPU computation
    withContext(Dispatchers.IO) { block() }
} catch (e: CancellationException) {
    // grpc-kotlin triggers coroutine cancel which eventually grpc-java ignores since
    // the connection already gone.
    // Typically, this is handled for us but since not using @GrpcAdvice/@GrpcExceptionHandler
    // framework since I'm too late to the party then we just catch this here for simplicity.
    throw e
} catch (e: Exception) {
    throw toStatusException(e)
}

// Validate through protovalidate. All validation errors is joined with "; ".
private fun requireValid(request: Message) {
    val violations = validator.validate(request).violations
    if (violations.isNotEmpty()) throw ValidationException(violations.joinToString("; ") { it.toProto().message })
}

internal fun toStatusException(e: Exception): StatusException = when (e) {
    is ValidationException -> StatusException(Status.INVALID_ARGUMENT.withDescription(e.message))
    is NotFoundException -> StatusException(Status.NOT_FOUND.withDescription(e.message))
    is AlreadyExistsException -> StatusException(Status.ALREADY_EXISTS.withDescription(e.message))
    is OptimisticLockingFailureException ->
        StatusException(Status.ABORTED.withDescription("Someone else changed this at the same time. Reload and try again."))
    else -> {
        // Generic error
        logger.error("Unexpected error in a gRPC call", e)
        StatusException(Status.INTERNAL.withDescription("Something went wrong"))
    }
}

