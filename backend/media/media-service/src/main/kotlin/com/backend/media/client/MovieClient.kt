package com.backend.media.client

import com.backend.media.exception.MovieServiceUnavailableException
import com.backend.media.exception.NotFoundException
import com.backend.movie.api.MovieServiceGrpc
import com.backend.movie.api.attachArtworkRequest
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.springframework.grpc.client.GrpcChannelFactory
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val DEADLINE_SECONDS = 5L

/**
 * The call to movie-service. Services depend on this interface, so they never see movie-api classes
 * and tests can mock it.
 */
interface MovieClient {
    /** Records stored artwork on its movie. Returns the replaced file's key, or null if nothing was replaced. */
    fun attachArtwork(movieId: UUID, storageKey: String): String?
}

/** Uses the blocking stub, since it's called from ordinary (non-suspend) service functions. */
@Component
class GrpcMovieClient(channels: GrpcChannelFactory) : MovieClient {

    // "movie" is the channel's name; its address comes from spring.grpc.client.channel.movie.target.
    private val stub = MovieServiceGrpc.newBlockingStub(channels.createChannel("movie"))

    override fun attachArtwork(movieId: UUID, storageKey: String): String? {
        val request = attachArtworkRequest {
            this.movieId = movieId.toString()
            this.storageKey = storageKey
        }
        try {
            val response = stub.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS).attachArtwork(request)
            return if (response.hasReplacedStorageKey()) response.replacedStorageKey else null
        } catch (e: StatusRuntimeException) {
            // An unknown movie keeps its message; anything else becomes "unavailable".
            throw when (e.status.code) {
                Status.Code.NOT_FOUND -> NotFoundException(e.status.description.orEmpty())
                else -> MovieServiceUnavailableException(e)
            }
        }
    }
}
