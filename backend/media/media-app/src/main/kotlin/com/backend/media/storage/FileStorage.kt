package com.backend.media.storage

import com.backend.media.exception.NotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/**
 * The only code that touches the disk. Every file lives under [root].
 * A storage key is a file's path relative to the root, such as `{movieId}/{uuid}`.
 */
@Component
class FileStorage(@Value("\${media.storage.root}") root: String) {

    // Get full path to project root; converts all .. or . in path to full
    private val root: Path = Path.of(root).toAbsolutePath().normalize()

    // Returns /path/to/root/movieId/uuid
    fun newFile(movieId: UUID): Path {
        val folder = root.resolve(movieId.toString())
        Files.createDirectories(folder)
        return folder.resolve(UUID.randomUUID().toString())
    }

    // Convert to path relative to project root
    fun keyOf(file: Path): String = root.relativize(file).toString()

    fun size(storageKey: String): Long = Files.size(existingFile(storageKey))

    fun open(storageKey: String): InputStream = Files.newInputStream(existingFile(storageKey))

    fun delete(storageKey: String) {
        Files.deleteIfExists(resolve(storageKey))
    }

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
