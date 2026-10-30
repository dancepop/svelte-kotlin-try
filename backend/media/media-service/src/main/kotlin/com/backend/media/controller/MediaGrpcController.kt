package com.backend.media.controller

import com.backend.media.api.v1.DeleteFileRequest
import com.backend.media.api.v1.DeleteMovieFilesRequest
import com.backend.media.api.v1.MediaServiceGrpcKt
import com.backend.media.api.v1.ReadFileRequest
import com.backend.media.api.v1.ReadFileResponse
import com.backend.media.api.v1.UploadFileRequest
import com.backend.media.api.v1.UploadFileResponse
import com.backend.media.api.v1.UploadHeader
import com.backend.media.api.v1.fileInfo
import com.backend.media.api.v1.readFileResponse
import com.backend.media.api.v1.uploadFileResponse
import com.backend.media.enums.MediaKind
import com.backend.media.exception.ValidationException
import com.backend.media.service.Upload
import com.backend.media.service.UploadService
import com.backend.media.service.contentTypeOf
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
import com.backend.media.api.v1.MediaKind as ProtoMediaKind

private const val CHUNK_BYTES = 64 * 1024

/**
 * gRPC entry point for media-service (the gRPC equivalent of a REST controller).
 * Spring Boot registers every BindableService bean with the gRPC server.
 */
@GrpcService
class MediaGrpcController(
    private val uploadService: UploadService,
    private val storage: FileStorage,
) : MediaServiceGrpcKt.MediaServiceCoroutineImplBase() {

    override suspend fun uploadFile(requests: Flow<UploadFileRequest>): UploadFileResponse = rpc {
        var upload: Upload? = null
        try {
            // collect only asks for the next message once this one is written, which is the back-pressure.
            requests.collect { request ->
                when (request.payloadCase) {
                    UploadFileRequest.PayloadCase.HEADER -> {
                        if (upload != null) throw invalidStream("Only the first message can be a header")
                        upload = begin(request.header)
                    }
                    UploadFileRequest.PayloadCase.CHUNK -> {
                        val current = upload ?: throw invalidStream("The first message must be a header")
                        uploadService.write(current, request.chunk.toByteArray())
                    }
                    else -> throw invalidStream("A message must be a header or a chunk")
                }
            }
            val stored = uploadService.finish(upload ?: throw invalidStream("The first message must be a header"))
            uploadFileResponse {
                storageKey = stored.storageKey
                contentType = stored.contentType
                sizeBytes = stored.sizeBytes
                stored.durationSeconds?.let { durationSeconds = it }
            }
        } finally {
            // Runs for an interrupted upload too, so its temp file doesn't stay behind.
            upload?.let(uploadService::cleanUp)
        }
    }

    override fun readFile(request: ReadFileRequest): Flow<ReadFileResponse> = flow {
        if (request.offset < 0 || (request.hasLength() && request.length < 0)) {
            throw ValidationException("offset", "Offset and length can't be negative")
        }
        val totalSize = storage.size(request.storageKey)
        emit(
            readFileResponse {
                info = fileInfo {
                    contentType = contentTypeOf(request.storageKey)
                    this.totalSize = totalSize
                }
            },
        )
        var remaining = if (request.hasLength()) request.length else totalSize - request.offset
        storage.open(request.storageKey, request.offset).use { file ->
            val buffer = ByteBuffer.allocate(CHUNK_BYTES)
            while (remaining > 0) {
                buffer.clear().limit(minOf(CHUNK_BYTES.toLong(), remaining).toInt())
                val read = file.read(buffer)
                if (read <= 0) break
                remaining -= read
                emit(readFileResponse { chunk = ByteString.copyFrom(buffer.array(), 0, read) })
            }
        }
    }
        .flowOn(Dispatchers.IO)
        .catch { e ->
            if (e is CancellationException) throw e
            throw toStatusException(e)
        }

    override suspend fun deleteFile(request: DeleteFileRequest): Empty = rpc {
        storage.delete(request.storageKey)
        Empty.getDefaultInstance()
    }

    override suspend fun deleteMovieFiles(request: DeleteMovieFilesRequest): Empty = rpc {
        storage.deleteMovieFolder(parseMovieId(request.movieId))
        Empty.getDefaultInstance()
    }

    private fun begin(header: UploadHeader): Upload = uploadService.begin(
        parseMovieId(header.movieId),
        header.kind.toEnum(),
        if (header.hasDurationSeconds()) header.durationSeconds else null,
    )

    private fun parseMovieId(value: String): UUID = try {
        UUID.fromString(value)
    } catch (_: IllegalArgumentException) {
        throw ValidationException("movieId", "Not a valid id")
    }

    private fun ProtoMediaKind.toEnum(): MediaKind = when (this) {
        ProtoMediaKind.ARTWORK -> MediaKind.ARTWORK
        ProtoMediaKind.PREVIEW -> MediaKind.PREVIEW
        ProtoMediaKind.FULL -> MediaKind.FULL
        else -> throw ValidationException("kind", "Kind is required")
    }

    private fun invalidStream(message: String) = ValidationException("file", message)
}
