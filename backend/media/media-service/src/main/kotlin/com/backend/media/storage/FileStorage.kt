package com.backend.media.storage

import com.backend.media.exception.NotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.channels.ReadableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/**
 * The only code that touches the disk. Every file lives under [root].
 * A storage key is a file's path relative to the root, such as `{movieId}/{uuid}`.
 */
@Component
class FileStorage(@Value("\${media.storage.root}") root: String) {

    private val root: Path = Path.of(root).toAbsolutePath().normalize()

    /** A path for a new upload. Every file gets a new uuid, so a key never changes content. */
    fun newFile(movieId: UUID): Path {
        val folder = root.resolve(movieId.toString())
        Files.createDirectories(folder)
        return folder.resolve(UUID.randomUUID().toString())
    }

    fun keyOf(file: Path): String = root.relativize(file).toString()

    fun deleteUnfinishedFile(file: Path) {
        Files.deleteIfExists(file)
    }

    fun size(storageKey: String): Long = Files.size(existingFile(storageKey))

    /** Opens the file for reading. The caller closes it. */
    fun open(storageKey: String): ReadableByteChannel = Files.newByteChannel(existingFile(storageKey))

    /** Deleting a file that's already gone succeeds. */
    fun delete(storageKey: String) {
        Files.deleteIfExists(resolve(storageKey))
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

    // Keys come from outside, so `..` and absolute paths must not reach the disk.
    private fun resolve(storageKey: String): Path {
        val path = root.resolve(storageKey).normalize()
        if (!path.startsWith(root)) throw NotFoundException("File not found")
        return path
    }
}
