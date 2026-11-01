package com.backend.media.service

import com.backend.media.client.MovieApi
import com.backend.media.exception.MovieServiceUnavailableException
import com.backend.media.storage.FileStorage
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MediaServiceTests {

    @TempDir
    lateinit var tempDir: Path

    private val storage = mockk<FileStorage>()
    private val movieApi = mockk<MovieApi>()
    private val service = MediaService(storage, movieApi)

    private val movieId = UUID.randomUUID()
    private lateinit var file: Path

    @BeforeTest
    fun setUp() {
        // upload() writes to a real file, so newFile hands it one in the temp folder.
        file = tempDir.resolve("new")
        every { storage.newFile(movieId) } returns file
        every { storage.keyOf(file) } returns "movie/new"
        every { storage.delete(any()) } just runs
    }

    // The parts MediaGrpcController passes on for a normal upload.
    private suspend fun upload(): String =
        service.upload(flowOf(UploadPart.Start(movieId), UploadPart.Chunk("image".toByteArray())))

    @Test
    fun finishDeletesReplacedFile() = runTest {
        every { movieApi.attachArtwork(movieId, "movie/new") } returns "movie/old"

        assertEquals("movie/new", upload())

        verify { storage.delete("movie/old") }
        verify(exactly = 0) { storage.delete("movie/new") }
    }

    @Test
    fun finishWithNothingReplacedDeletesNothing() = runTest {
        every { movieApi.attachArtwork(movieId, "movie/new") } returns null

        upload()

        verify(exactly = 0) { storage.delete(any()) }
    }

    @Test
    fun attachFailsDeletesNewFile() = runTest {
        every { movieApi.attachArtwork(any(), any()) } throws MovieServiceUnavailableException(RuntimeException())

        assertFailsWith<MovieServiceUnavailableException> { upload() }

        verify { storage.delete("movie/new") }
    }

    @Test
    fun deletingReplacedFileFailsUploadStillSucceeds() = runTest {
        every { movieApi.attachArtwork(movieId, "movie/new") } returns "movie/old"
        every { storage.delete("movie/old") } throws RuntimeException("disk error")

        assertEquals("movie/new", upload())
    }
}
