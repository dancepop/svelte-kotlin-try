package com.backend.media.service

import com.backend.media.client.MovieApi
import com.backend.media.exception.ValidationException
import com.backend.media.storage.FileStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

private const val CHUNK_BYTES = 64 * 1024

// One message of an upload
sealed interface UploadPart {
    data class Start(val movieId: UUID) : UploadPart
    class Chunk(val bytes: ByteArray) : UploadPart
}

/** A file on its way in, from the Start part until the stream ends. */
private class Upload(
    val movieId: UUID,
    val file: Path,
    val output: OutputStream,
) {
    var attached = false
}

@Service
class MediaService(
    private val storage: FileStorage,
    private val movieApi: MovieApi,
) {
    private val logger = LoggerFactory.getLogger(MediaService::class.java)

    /** Writes the parts to a new file, attaches it to the movie, and returns its storage key. */
    suspend fun upload(parts: Flow<UploadPart>): String {
        var upload: Upload? = null
        try {
            // collect only asks for the next part once this one is written, which is the back-pressure.
            parts.collect { part ->
                when (part) {
                    is UploadPart.Start -> {
                        if (upload != null) throw ValidationException("Only the first message can be the movie id")
                        val file = storage.newFile(part.movieId)
                        upload = Upload(part.movieId, file, Files.newOutputStream(file))
                    }
                    is UploadPart.Chunk -> {
                        val current = upload ?: throw ValidationException("The first message must be the movie id")
                        current.output.write(part.bytes)
                    }
                }
            }
            // The client has finished sending. upload is still null if it sent no messages.
            val current = upload ?: throw ValidationException("The first message must be the movie id")
            return finish(current)
        } finally {
            // Runs however the upload ends. Deletes the file unless a movie points at it.
            upload?.let {
                it.output.close()
                if (!it.attached) storage.delete(storage.keyOf(it.file))
            }
        }
    }

    fun size(storageKey: String): Long = storage.size(storageKey)

    /** The file's bytes, CHUNK_BYTES at a time. */
    fun read(storageKey: String): Flow<ByteArray> = flow {
        storage.open(storageKey).use { input ->
            val buffer = ByteArray(CHUNK_BYTES)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                emit(buffer.copyOf(read))
            }
        }
    }

    fun deleteMovieFiles(movieId: UUID) {
        storage.deleteMovieFolder(movieId)
    }

    // Replace file location in movie table and remove old file
    private fun finish(upload: Upload): String {
        upload.output.close()
        val storageKey = storage.keyOf(upload.file)
        val replacedKey = movieApi.attachArtwork(upload.movieId, storageKey)
        upload.attached = true
        if (replacedKey != null) {
            // The worst a failure here can leave is a file nobody points at.
            try {
                storage.delete(replacedKey)
            } catch (e: Exception) {
                logger.warn("Couldn't delete file {}", replacedKey, e)
            }
        }
        return storageKey
    }
}
