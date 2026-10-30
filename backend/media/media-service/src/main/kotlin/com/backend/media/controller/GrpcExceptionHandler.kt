package com.backend.media.controller

import com.backend.media.exception.MovieServiceUnavailableException
import com.backend.media.exception.NotFoundException
import com.backend.media.exception.PublishedMovieException
import com.backend.media.exception.ValidationException
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.backend.media.controller.GrpcExceptionHandler")

// Sent as "field=message", so the gateway can show the message next to its field.
private val FIELD_ERROR_KEY = Metadata.Key.of("field-error", Metadata.ASCII_STRING_MARSHALLER)

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

internal fun toStatusException(e: Throwable): StatusException = when (e) {
    is ValidationException -> {
        val trailers = Metadata()
        trailers.put(FIELD_ERROR_KEY, "${e.field}=${e.message}")
        StatusException(Status.INVALID_ARGUMENT.withDescription(e.message), trailers)
    }
    is NotFoundException -> StatusException(Status.NOT_FOUND.withDescription(e.message))
    is PublishedMovieException -> StatusException(Status.FAILED_PRECONDITION.withDescription(e.message))
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
