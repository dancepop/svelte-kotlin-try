package com.backend.media.service

import com.backend.media.client.MovieClient
import com.backend.media.exception.MovieServiceUnavailableException
import com.backend.media.storage.FileStorage
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UploadServiceTests {

    @TempDir
    lateinit var tempDir: Path

    private val storage = mockk<FileStorage>()
    private val movieClient = mockk<MovieClient>()
    private val service = UploadService(storage, movieClient)

    private val movieId = UUID.randomUUID()
    private lateinit var file: Path

    @BeforeTest
    fun setUp() {
        // begin() writes to a real file, so newFile hands it one in the temp folder.
        file = tempDir.resolve("new")
        every { storage.newFile(movieId) } returns file
        every { storage.keyOf(file) } returns "movie/new"
        every { storage.delete(any()) } just runs
        every { storage.deleteUnfinishedFile(any()) } just runs
    }

    // The flow MediaGrpcController runs: begin, write, finish, and always cleanUp.
    private fun upload(): String {
        val upload = service.begin(movieId)
        try {
            service.write(upload, "image".toByteArray())
            return service.finish(upload)
        } finally {
            service.cleanUp(upload)
        }
    }

    @Test
    fun finishDeletesReplacedFile() {
        every { movieClient.attachArtwork(movieId, "movie/new") } returns "movie/old"

        assertEquals("movie/new", upload())

        verify { storage.delete("movie/old") }
        verify(exactly = 0) { storage.deleteUnfinishedFile(any()) }
    }

    @Test
    fun finishWithNothingReplacedDeletesNothing() {
        every { movieClient.attachArtwork(movieId, "movie/new") } returns null

        upload()

        verify(exactly = 0) { storage.delete(any()) }
        verify(exactly = 0) { storage.deleteUnfinishedFile(any()) }
    }

    @Test
    fun attachFailsDeletesNewFile() {
        every { movieClient.attachArtwork(any(), any()) } throws MovieServiceUnavailableException(RuntimeException())

        assertFailsWith<MovieServiceUnavailableException> { upload() }

        verify { storage.deleteUnfinishedFile(file) }
    }

    @Test
    fun deletingReplacedFileFailsUploadStillSucceeds() {
        every { movieClient.attachArtwork(movieId, "movie/new") } returns "movie/old"
        every { storage.delete("movie/old") } throws RuntimeException("disk error")

        assertEquals("movie/new", upload())
    }
}
