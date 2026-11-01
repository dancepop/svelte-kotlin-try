package com.backend.media.controller

import com.backend.media.api.UploadFileRequest
import com.backend.media.api.readFileRequest
import com.backend.media.api.uploadFileRequest
import com.backend.media.client.MovieApi
import com.backend.media.exception.MovieServiceUnavailableException
import com.backend.media.service.MediaService
import com.backend.media.storage.FileStorage
import com.google.protobuf.ByteString
import io.grpc.Status
import io.grpc.StatusException
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MediaGrpcControllerTests {

    @TempDir
    lateinit var tempDir: Path

    private val movieApi = mockk<MovieApi>()
    private lateinit var storage: FileStorage
    private lateinit var controller: MediaGrpcController

    private val movieId = UUID.randomUUID()

    @BeforeTest
    fun setUp() {
        storage = FileStorage(tempDir.toString())
        controller = MediaGrpcController(MediaService(storage, movieApi))
        every { movieApi.attachArtwork(any(), any()) } returns null
    }

    private fun movieIdMessage(id: String = movieId.toString()) = uploadFileRequest { movieId = id }

    private fun chunk(text: String) = uploadFileRequest { chunk = ByteString.copyFromUtf8(text) }

    // The message a rejected upload carries.
    private suspend fun rejectionOf(vararg messages: UploadFileRequest): String {
        val error = assertFailsWith<StatusException> { controller.uploadFile(flowOf(*messages)) }
        assertEquals(Status.Code.INVALID_ARGUMENT, error.status.code)
        return error.status.description.orEmpty()
    }

    private fun filesOnDisk(): List<Path> = Files.walk(tempDir).use { paths -> paths.filter(Files::isRegularFile).toList() }

    @Test
    fun uploadFile() = runTest {
        val response = controller.uploadFile(flowOf(movieIdMessage(), chunk("ima"), chunk("ge")))

        assertTrue(response.storageKey.startsWith("$movieId/"))
        assertEquals(5, storage.size(response.storageKey))
    }

    @Test
    fun malformedMovieId() = runTest {
        rejectionOf(movieIdMessage("42"))
    }

    @Test
    fun emptyStream() = runTest {
        assertEquals("The first message must be the movie id", rejectionOf())
    }

    @Test
    fun chunkBeforeMovieId() = runTest {
        assertEquals("The first message must be the movie id", rejectionOf(chunk("image"), movieIdMessage()))
    }

    @Test
    fun twoMovieIds() = runTest {
        assertEquals(
            "Only the first message can be the movie id",
            rejectionOf(movieIdMessage(), movieIdMessage()),
        )
    }

    @Test
    fun cancelledUploadLeavesNoFile() = runTest {
        val cancelledPartway = flow {
            emit(movieIdMessage())
            emit(chunk("ima"))
            throw CancellationException("Cancellation received from client")
        }

        assertFailsWith<CancellationException> { controller.uploadFile(cancelledPartway) }
        assertTrue(filesOnDisk().isEmpty())
    }

    @Test
    fun movieAppDownIsUnavailableAndLeavesNoFile() = runTest {
        every { movieApi.attachArtwork(any(), any()) } throws MovieServiceUnavailableException(RuntimeException())

        val error = assertFailsWith<StatusException> { controller.uploadFile(flowOf(movieIdMessage(), chunk("image"))) }

        assertEquals(Status.Code.UNAVAILABLE, error.status.code)
        assertTrue(filesOnDisk().isEmpty())
    }

    @Test
    fun readFile() = runTest {
        // Over 64 KB, so it arrives in more than one chunk.
        val bytes = Random.nextBytes(100_000)
        val key = storage.keyOf(Files.write(storage.newFile(movieId), bytes))

        val messages = controller.readFile(readFileRequest { storageKey = key }).toList()

        assertEquals(100_000L, messages.first().info.totalSize)
        assertTrue(messages.size > 2)
        val received = messages.drop(1).fold(ByteString.EMPTY) { all, message -> all.concat(message.chunk) }
        assertContentEquals(bytes, received.toByteArray())
    }

    @Test
    fun readMissingFile() = runTest {
        val error = assertFailsWith<StatusException> {
            controller.readFile(readFileRequest { storageKey = "$movieId/missing" }).toList()
        }

        assertEquals(Status.Code.NOT_FOUND, error.status.code)
    }
}
