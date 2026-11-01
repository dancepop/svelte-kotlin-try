package com.backend.movie.client

import com.backend.media.api.MediaServiceGrpc
import com.backend.media.api.deleteMovieFilesRequest
import org.springframework.grpc.client.GrpcChannelFactory
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val DEADLINE_SECONDS = 5L

interface MediaApi {
    fun deleteMovieFiles(movieId: UUID)
}

@Component
class GrpcMediaApi(channels: GrpcChannelFactory) : MediaApi {
    private val stub = MediaServiceGrpc.newBlockingStub(channels.createChannel("media"))

    override fun deleteMovieFiles(movieId: UUID) {
        stub.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
            .deleteMovieFiles(deleteMovieFilesRequest { this.movieId = movieId.toString() })
    }
}
