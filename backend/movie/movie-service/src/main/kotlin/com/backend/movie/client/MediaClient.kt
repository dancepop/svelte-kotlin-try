package com.backend.movie.client

import com.backend.media.api.v1.MediaServiceGrpc
import com.backend.media.api.v1.deleteFileRequest
import com.backend.media.api.v1.deleteMovieFilesRequest
import org.springframework.grpc.client.GrpcChannelFactory
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val DEADLINE_SECONDS = 5L

/**
 * The calls to media-service. Services depend on this interface, so they never see media-api classes
 * and tests can swap in a fake.
 */
interface MediaClient {
    fun deleteFile(storageKey: String)
    fun deleteMovieFiles(movieId: UUID)
}

/** Uses the blocking stub, since it's called from ordinary (non-suspend) service functions. */
@Component
class GrpcMediaClient(channels: GrpcChannelFactory) : MediaClient {

    // "media" is the channel's name; its address comes from spring.grpc.client.channel.media.target.
    private val stub = MediaServiceGrpc.newBlockingStub(channels.createChannel("media"))

    override fun deleteFile(storageKey: String) {
        stub.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
            .deleteFile(deleteFileRequest { this.storageKey = storageKey })
    }

    override fun deleteMovieFiles(movieId: UUID) {
        stub.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
            .deleteMovieFiles(deleteMovieFilesRequest { this.movieId = movieId.toString() })
    }
}
