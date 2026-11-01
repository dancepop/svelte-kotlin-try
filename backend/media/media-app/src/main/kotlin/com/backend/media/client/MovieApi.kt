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

interface MovieApi {
    fun attachArtwork(movieId: UUID, storageKey: String): String?
}

@Component
class GrpcMovieApi(channels: GrpcChannelFactory) : MovieApi {
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
            throw when (e.status.code) {
                Status.Code.NOT_FOUND -> NotFoundException(e.status.description.orEmpty())
                else -> MovieServiceUnavailableException(e)
            }
        }
    }
}
