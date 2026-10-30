package com.backend.movie.controller

import com.backend.movie.exception.AlreadyExistsException
import com.backend.movie.exception.ConflictException
import com.backend.movie.exception.NotFoundException
import com.backend.movie.exception.PublishedMovieException
import com.backend.movie.exception.ValidationException
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.backend.movie.controller.GrpcExceptionHandler")

// Sent once per broken rule as "field=message", so the gateway can show each message next to its field.
private val FIELD_ERROR_KEY = Metadata.Key.of("field-error", Metadata.ASCII_STRING_MARSHALLER)

/**
 * Wraps every controller method. Runs blocking work on the coroutine pool meant for it, and turns exceptions
 * into gRPC statuses. Spring's exception handling can't do this here: grpc-kotlin catches exceptions inside
 * the coroutine, so they'd reach the client as UNKNOWN.
 */
internal suspend fun <T> rpc(block: () -> T): T = try {
    withContext(Dispatchers.IO) { block() }
} catch (e: CancellationException) {
    // The client went away or its deadline passed. This must stay a cancellation.
    throw e
} catch (e: Exception) {
    throw toStatusException(e)
}

internal fun toStatusException(e: Exception): StatusException = when (e) {
    is ValidationException -> {
        val trailers = Metadata()
        e.fieldErrors.forEach { trailers.put(FIELD_ERROR_KEY, "${it.field}=${it.message}") }
        StatusException(Status.INVALID_ARGUMENT.withDescription(e.message), trailers)
    }
    is NotFoundException -> StatusException(Status.NOT_FOUND.withDescription(e.message))
    is PublishedMovieException -> StatusException(Status.FAILED_PRECONDITION.withDescription(e.message))
    is ConflictException -> StatusException(Status.ABORTED.withDescription(e.message))
    is AlreadyExistsException -> StatusException(Status.ALREADY_EXISTS.withDescription(e.message))
    else -> {
        // Details stay in the logs; the caller only learns that it failed.
        logger.error("Unexpected error in a gRPC call", e)
        StatusException(Status.INTERNAL.withDescription("Something went wrong"))
    }
}
