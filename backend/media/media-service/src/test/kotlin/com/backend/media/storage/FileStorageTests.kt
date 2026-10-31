package com.backend.media.storage

import com.backend.media.exception.NotFoundException
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileStorageTests {

    // JUnit creates a fresh folder for each test and deletes it afterwards.
    @TempDir
    lateinit var tempDir: Path

    private lateinit var root: Path
    private lateinit var storage: FileStorage

    // A file next to the media root, which no key may reach.
    private lateinit var outside: Path

    @BeforeTest
    fun setUp() {
        root = tempDir.resolve("media")
        storage = FileStorage(root.toString())
        outside = Files.writeString(tempDir.resolve("secret"), "secret")
    }

    @Test
    fun newFileKeyFindsTheFile() {
        val movieId = UUID.randomUUID()
        val file = Files.writeString(storage.newFile(movieId), "image")

        val key = storage.keyOf(file)

        assertTrue(key.startsWith("$movieId/"))
        assertEquals(5, storage.size(key))
    }

    @Test
    fun keyWithDotDotIsRejected() {
        assertFailsWith<NotFoundException> { storage.size("../secret") }
        assertFailsWith<NotFoundException> { storage.delete("../secret") }
        assertTrue(Files.exists(outside))
    }

    @Test
    fun absolutePathIsRejected() {
        assertFailsWith<NotFoundException> { storage.size(outside.toString()) }
    }

    @Test
    fun missingFileIsNotFound() {
        assertFailsWith<NotFoundException> { storage.size("${UUID.randomUUID()}/${UUID.randomUUID()}") }
    }

    @Test
    fun deleteMissingFile() {
        storage.delete("${UUID.randomUUID()}/${UUID.randomUUID()}")
    }

    @Test
    fun deleteMovieFolder() {
        val movieId = UUID.randomUUID()
        Files.writeString(storage.newFile(movieId), "one")
        Files.writeString(storage.newFile(movieId), "two")

        storage.deleteMovieFolder(movieId)
        storage.deleteMovieFolder(movieId) // already gone, still succeeds

        assertFalse(Files.exists(root.resolve(movieId.toString())))
    }
}
