package com.backend.media.service

import com.backend.media.client.MovieClient
import com.backend.media.storage.FileStorage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/** A file on its way in. The upload flow keeps it from the first message until the stream ends. */
class Upload(
    val movieId: UUID,
    val file: Path,
    val output: OutputStream,
) {
    var attached = false
}

@Service
class UploadService(
    private val storage: FileStorage,
    private val movieClient: MovieClient,
) {
    private val logger = LoggerFactory.getLogger(UploadService::class.java)

    fun begin(movieId: UUID): Upload {
        val file = storage.newFile(movieId)
        return Upload(movieId, file, Files.newOutputStream(file))
    }

    fun write(upload: Upload, chunk: ByteArray) {
        upload.output.write(chunk)
    }

    /** The stream ended: record the file, then remove the one it replaced. */
    fun finish(upload: Upload): String {
        upload.output.close()
        val storageKey = storage.keyOf(upload.file)
        // If this fails, the movie keeps its previous file and cleanUp removes this new one.
        val replacedKey = movieClient.attachArtwork(upload.movieId, storageKey)
        upload.attached = true
        // The row stopped pointing at the old file during the attach.
        if (replacedKey != null) deleteQuietly(replacedKey)
        return storageKey
    }

    /** Always called when an upload ends. Removes the file unless a movie now points at it. */
    fun cleanUp(upload: Upload) {
        upload.output.close()
        if (!upload.attached) storage.deleteUnfinishedFile(upload.file)
    }

    // The worst a failure here can leave is a file nobody points at.
    private fun deleteQuietly(storageKey: String) {
        try {
            storage.delete(storageKey)
        } catch (e: Exception) {
            logger.warn("Couldn't delete file {}", storageKey, e)
        }
    }
}
