package com.backend.media.controller

import com.backend.media.api.DeleteMovieFilesRequest
import com.backend.media.api.MediaServiceGrpcKt
import com.backend.media.api.ReadFileRequest
import com.backend.media.api.ReadFileResponse
import com.backend.media.api.UploadFileRequest
import com.backend.media.api.UploadFileResponse
import com.backend.media.api.fileInfo
import com.backend.media.api.readFileResponse
import com.backend.media.api.uploadFileResponse
import com.backend.media.exception.ValidationException
import com.backend.media.service.MediaService
import com.backend.media.service.UploadPart
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.springframework.grpc.server.service.GrpcService
import java.util.UUID

@GrpcService
class MediaGrpcController(
    private val mediaService: MediaService,
) : MediaServiceGrpcKt.MediaServiceCoroutineImplBase() {

    override suspend fun uploadFile(requests: Flow<UploadFileRequest>): UploadFileResponse = rpc {
        val key = mediaService.upload(requests.map { it.toUploadPart() })
        uploadFileResponse { storageKey = key }
    }

    override fun readFile(request: ReadFileRequest): Flow<ReadFileResponse> = flow {
        emit(readFileResponse { info = fileInfo { totalSize = mediaService.size(request.storageKey) } })
        emitAll(mediaService.read(request.storageKey).map { readFileResponse { chunk = ByteString.copyFrom(it) } })
    }
        .flowOn(Dispatchers.IO)
        .catch { e ->
            if (e is CancellationException) throw e
            throw toStatusException(e)
        }

    override suspend fun deleteMovieFiles(request: DeleteMovieFilesRequest): Empty = rpc {
        requireValid(request)
        mediaService.deleteMovieFiles(UUID.fromString(request.movieId))
        Empty.getDefaultInstance()
    }

    // Checks one message against its .proto rules and converts it, so the service never sees proto classes.
    private fun UploadFileRequest.toUploadPart(): UploadPart {
        requireValid(this)
        return when (payloadCase) {
            UploadFileRequest.PayloadCase.MOVIE_ID -> UploadPart.Start(UUID.fromString(movieId))
            UploadFileRequest.PayloadCase.CHUNK -> UploadPart.Chunk(chunk.toByteArray())
            else -> throw ValidationException("A message must be a movie id or a chunk")
        }
    }
}
