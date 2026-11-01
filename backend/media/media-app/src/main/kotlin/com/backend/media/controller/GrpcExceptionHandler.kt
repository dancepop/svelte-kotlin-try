package com.backend.media.controller

import com.backend.media.exception.MovieServiceUnavailableException
import com.backend.media.exception.NotFoundException
import build.buf.protovalidate.ValidatorFactory
import com.backend.media.exception.ValidationException
import com.google.protobuf.Message
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.backend.media.controller.GrpcExceptionHandler")

private val validator = ValidatorFactory.newBuilder().build()

/**
 * Wraps controller methods. Runs blocking work on the coroutine pool meant for it, and turns exceptions into
 * gRPC statuses.
 */
internal suspend fun <T> rpc(block: suspend () -> T): T = try {
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

internal fun requireValid(request: Message) {
    // Each media message has at most one rule
    val violation = validator.validate(request).violations.firstOrNull() ?: return
    throw ValidationException(violation.toProto().message)
}

internal fun toStatusException(e: Throwable): StatusException = when (e) {
    is ValidationException -> StatusException(Status.INVALID_ARGUMENT.withDescription(e.message))
    is NotFoundException -> StatusException(Status.NOT_FOUND.withDescription(e.message))
    is MovieServiceUnavailableException -> {
        logger.warn("Couldn't reach movie-app", e)
        StatusException(Status.UNAVAILABLE.withDescription("Something went wrong, try again"))
    }
    else -> {
        // Details stay in the logs; the caller only learns that it failed.
        logger.error("Unexpected error in a gRPC call", e)
        StatusException(Status.INTERNAL.withDescription("Something went wrong"))
    }
}

