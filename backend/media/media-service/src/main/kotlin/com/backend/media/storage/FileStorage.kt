package com.backend.media.storage

import com.backend.media.exception.NotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

private const val TEMP_FOLDER = "tmp"

/**
 * The only code that touches the disk. Every file lives under [root], which is the media-data volume in compose.
 * A storage key is a file's path relative to the root, such as `{movieId}/artwork/{uuid}.jpg`.
 */
@Component
class FileStorage(@Value("\${media.storage.root}") root: String) {

    private val root: Path = Path.of(root).toAbsolutePath().normalize()

    /** A path for a file still being uploaded. A half-written file never appears under a real key. */
    fun newTempFile(): Path {
        val folder = root.resolve(TEMP_FOLDER)
        Files.createDirectories(folder)
        return folder.resolve(UUID.randomUUID().toString())
    }

    /** Puts a finished upload in place and returns its key. Every file gets a new uuid, so a key never changes content. */
    fun moveIntoPlace(tempFile: Path, movieId: UUID, folder: String, extension: String): String {
        val target = root.resolve(movieId.toString()).resolve(folder).resolve("${UUID.randomUUID()}.$extension")
        Files.createDirectories(target.parent)
        // Temp files are on the same volume, so the move is atomic.
        Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE)
        return root.relativize(target).toString()
    }

    fun deleteTempFile(tempFile: Path) {
        Files.deleteIfExists(tempFile)
    }

    fun size(storageKey: String): Long = Files.size(existingFile(storageKey))

    /** Opens the file for reading, already at [offset]. The caller closes it. */
    fun open(storageKey: String, offset: Long): SeekableByteChannel =
        Files.newByteChannel(existingFile(storageKey)).position(offset)

    /** Deleting a file that's already gone succeeds. A key that points at a folder is not found. */
    fun delete(storageKey: String) {
        val path = resolve(storageKey)
        if (Files.isDirectory(path)) throw NotFoundException("File not found")
        Files.deleteIfExists(path)
    }

    /** Grouping files by movie makes this one folder delete. Deleting a folder that's already gone succeeds. */
    fun deleteMovieFolder(movieId: UUID) {
        root.resolve(movieId.toString()).toFile().deleteRecursively()
    }

    private fun existingFile(storageKey: String): Path {
        val path = resolve(storageKey)
        if (!Files.isRegularFile(path)) throw NotFoundException("File not found")
        return path
    }

    // Keys come from outside, so `..`, absolute paths and unfinished uploads must not reach the disk.
    private fun resolve(storageKey: String): Path {
        val path = root.resolve(storageKey).normalize()
        if (!path.startsWith(root) || path.startsWith(root.resolve(TEMP_FOLDER))) {
            throw NotFoundException("File not found")
        }
        return path
    }
}
