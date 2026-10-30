package com.backend.media.service

import com.backend.media.client.MovieClient
import com.backend.media.enums.MediaKind
import com.backend.media.exception.ValidationException
import com.backend.media.storage.FileStorage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/** A file on its way in. The upload flow keeps it from the first message until the stream ends. */
class Upload(
    val movieId: UUID,
    val kind: MediaKind,
    val durationSeconds: Int?,
    val tempFile: Path,
    val output: OutputStream,
) {
    var sizeBytes = 0L
    var fileType: FileType? = null
    val head = ByteArrayOutputStream(SIGNATURE_LENGTH)
}

/** A file that's stored and recorded on its movie. */
data class StoredFile(
    val storageKey: String,
    val contentType: String,
    val sizeBytes: Long,
    val durationSeconds: Int?,
)

@Service
class UploadService(
    private val storage: FileStorage,
    private val movieClient: MovieClient,
) {
    private val logger = LoggerFactory.getLogger(UploadService::class.java)

    fun begin(movieId: UUID, kind: MediaKind, durationSeconds: Int?): Upload {
        val tempFile = storage.newTempFile()
        return Upload(movieId, kind, durationSeconds, tempFile, Files.newOutputStream(tempFile))
    }

    /** Checks as bytes arrive, so a bad file is rejected without waiting for the rest of it. */
    fun write(upload: Upload, chunk: ByteArray) {
        upload.sizeBytes += chunk.size
        if (upload.sizeBytes > upload.kind.maxBytes) {
            throw ValidationException("file", "The file must be ${upload.kind.maxBytes / (1024 * 1024)} MB or smaller")
        }
        // A chunk can be any size, so collect bytes until there are enough to identify the file.
        if (upload.fileType == null && upload.head.size() < SIGNATURE_LENGTH) {
            upload.head.write(chunk, 0, minOf(chunk.size, SIGNATURE_LENGTH - upload.head.size()))
            if (upload.head.size() == SIGNATURE_LENGTH) upload.fileType = checkedType(upload)
        }
        upload.output.write(chunk)
    }

    /** The stream ended: move the file into place, record it, then remove whichever file no row points at. */
    fun finish(upload: Upload): StoredFile {
        upload.output.close()
        // Only still unknown if the whole file is shorter than a signature, which fails this check.
        val fileType = upload.fileType ?: checkedType(upload)
        val storageKey = storage.moveIntoPlace(upload.tempFile, upload.movieId, upload.kind.folder, fileType.extension)
        val replacedKey = try {
            movieClient.attachMedia(
                upload.movieId,
                upload.kind,
                storageKey,
                fileType.contentType,
                upload.sizeBytes,
                upload.durationSeconds,
            )
        } catch (e: Exception) {
            // The movie keeps its previous file; this new one is the one nothing points at.
            deleteQuietly(storageKey)
            throw e
        }
        // The row stopped pointing at the old file during the attach.
        if (replacedKey != null) deleteQuietly(replacedKey)
        return StoredFile(storageKey, fileType.contentType, upload.sizeBytes, upload.durationSeconds)
    }

    /** Always called when an upload ends. After a successful finish, the temp file has already moved. */
    fun cleanUp(upload: Upload) {
        upload.output.close()
        storage.deleteTempFile(upload.tempFile)
    }

    private fun checkedType(upload: Upload): FileType {
        val fileType = detectFileType(upload.head.toByteArray())
        if (fileType == null || fileType.contentType !in upload.kind.allowedTypes) {
            throw ValidationException("file", "This file type isn't allowed for ${upload.kind.folder}")
        }
        return fileType
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
