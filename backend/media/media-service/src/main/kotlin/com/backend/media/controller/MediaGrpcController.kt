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
import com.backend.media.service.Upload
import com.backend.media.service.UploadService
import com.backend.media.storage.FileStorage
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.springframework.grpc.server.service.GrpcService
import java.nio.ByteBuffer
import java.util.UUID

private const val CHUNK_BYTES = 64 * 1024

/**
 * gRPC entry point for media-service (the gRPC equivalent of a REST controller). Each request message is checked
 * against the rules in the .proto with [requireValid] before it's used.
 * Spring Boot registers every BindableService bean with the gRPC server.
 */
@GrpcService
class MediaGrpcController(
    private val uploadService: UploadService,
    private val storage: FileStorage,
) : MediaServiceGrpcKt.MediaServiceCoroutineImplBase() {

    override suspend fun uploadFile(requests: Flow<UploadFileRequest>): UploadFileResponse = rpc {
        var upload: Upload? = null
        var complete = false
        try {
            // collect only asks for the next message once this one is written, which is the back-pressure.
            requests.collect { request ->
                requireValid(request)
                when (request.payloadCase) {
                    UploadFileRequest.PayloadCase.MOVIE_ID -> {
                        if (upload != null) throw invalidStream("Only the first message can be the movie id")
                        upload = uploadService.begin(UUID.fromString(request.movieId))
                    }
                    UploadFileRequest.PayloadCase.CHUNK -> {
                        val current = upload ?: throw invalidStream("The first message must be the movie id")
                        uploadService.write(current, request.chunk.toByteArray())
                    }
                    UploadFileRequest.PayloadCase.COMPLETE -> complete = true
                    else -> throw invalidStream("A message must be a movie id, a chunk or complete")
                }
            }
            val current = upload ?: throw invalidStream("The first message must be the movie id")
            // A cancelled client can look like a normal end of stream for a moment, so only `complete` proves the file is whole.
            if (!complete) throw invalidStream("The upload ended before the whole file arrived")
            val key = uploadService.finish(current)
            uploadFileResponse { storageKey = key }
        } finally {
            // Runs for an interrupted upload too, so its unfinished file doesn't stay behind.
            upload?.let(uploadService::cleanUp)
        }
    }

    override fun readFile(request: ReadFileRequest): Flow<ReadFileResponse> = flow {
        val size = storage.size(request.storageKey)
        emit(
            readFileResponse {
                info = fileInfo { totalSize = size }
            },
        )
        storage.open(request.storageKey).use { file ->
            val buffer = ByteBuffer.allocate(CHUNK_BYTES)
            while (true) {
                buffer.clear()
                val read = file.read(buffer)
                if (read <= 0) break
                emit(readFileResponse { chunk = ByteString.copyFrom(buffer.array(), 0, read) })
            }
        }
    }
        .flowOn(Dispatchers.IO)
        .catch { e ->
            if (e is CancellationException) throw e
            throw toStatusException(e)
        }

    override suspend fun deleteMovieFiles(request: DeleteMovieFilesRequest): Empty = rpc {
        requireValid(request)
        storage.deleteMovieFolder(UUID.fromString(request.movieId))
        Empty.getDefaultInstance()
    }

    private fun invalidStream(message: String) = ValidationException("file", message)
}
