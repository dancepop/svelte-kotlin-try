package com.backend.media.client

import com.backend.media.enums.MediaKind
import com.backend.media.exception.MovieServiceUnavailableException
import com.backend.media.exception.NotFoundException
import com.backend.media.exception.PublishedMovieException
import com.backend.media.exception.ValidationException
import com.backend.movie.api.v1.MovieServiceGrpc
import com.backend.movie.api.v1.attachMediaRequest
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.springframework.grpc.client.GrpcChannelFactory
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.TimeUnit
import com.backend.movie.api.v1.MediaKind as ProtoMediaKind

private const val DEADLINE_SECONDS = 5L

/**
 * The call to movie-service. Services depend on this interface, so they never see movie-api classes
 * and tests can swap in a fake.
 */
interface MovieClient {
    /** Records a stored file on its movie. Returns the replaced file's key, or null if nothing was replaced. */
    fun attachMedia(
        movieId: UUID,
        kind: MediaKind,
        storageKey: String,
        contentType: String,
        sizeBytes: Long,
        durationSeconds: Int?,
    ): String?
}

/** Uses the blocking stub, since it's called from ordinary (non-suspend) service functions. */
@Component
class GrpcMovieClient(channels: GrpcChannelFactory) : MovieClient {

    // "movie" is the channel's name; its address comes from spring.grpc.client.channel.movie.target.
    private val stub = MovieServiceGrpc.newBlockingStub(channels.createChannel("movie"))

    override fun attachMedia(
        movieId: UUID,
        kind: MediaKind,
        storageKey: String,
        contentType: String,
        sizeBytes: Long,
        durationSeconds: Int?,
    ): String? {
        val request = attachMediaRequest {
            this.movieId = movieId.toString()
            this.kind = kind.toProto()
            this.storageKey = storageKey
            this.contentType = contentType
            this.sizeBytes = sizeBytes
            durationSeconds?.let { this.durationSeconds = it }
        }
        try {
            val response = stub.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS).attachMedia(request)
            return if (response.hasReplacedStorageKey()) response.replacedStorageKey else null
        } catch (e: StatusRuntimeException) {
            throw translate(e)
        }
    }

    // Errors the user can act on keep their message; anything else becomes "unavailable".
    private fun translate(e: StatusRuntimeException): Exception {
        val message = e.status.description.orEmpty()
        return when (e.status.code) {
            Status.Code.INVALID_ARGUMENT -> ValidationException("file", message)
            Status.Code.NOT_FOUND -> NotFoundException(message)
            Status.Code.FAILED_PRECONDITION -> PublishedMovieException(message)
            else -> MovieServiceUnavailableException(e)
        }
    }

    private fun MediaKind.toProto(): ProtoMediaKind = when (this) {
        MediaKind.ARTWORK -> ProtoMediaKind.ARTWORK
        MediaKind.PREVIEW -> ProtoMediaKind.PREVIEW
        MediaKind.FULL -> ProtoMediaKind.FULL
    }
}
