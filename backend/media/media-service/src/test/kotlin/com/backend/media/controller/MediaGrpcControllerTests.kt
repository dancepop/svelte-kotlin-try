package com.backend.media.controller

import com.backend.media.api.UploadFileRequest
import com.backend.media.api.readFileRequest
import com.backend.media.api.uploadFileRequest
import com.backend.media.client.MovieClient
import com.backend.media.service.UploadService
import com.backend.media.storage.FileStorage
import com.google.protobuf.ByteString
import com.google.rpc.BadRequest
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.protobuf.StatusProto
import io.mockk.every
import io.mockk.mockk
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

// Real storage on a temp folder; only movie-service is mocked.
class MediaGrpcControllerTests {

    @TempDir
    lateinit var tempDir: Path

    private val movieClient = mockk<MovieClient>()
    private lateinit var storage: FileStorage
    private lateinit var controller: MediaGrpcController

    private val movieId = UUID.randomUUID()

    @BeforeTest
    fun setUp() {
        storage = FileStorage(tempDir.toString())
        controller = MediaGrpcController(UploadService(storage, movieClient), storage)
        every { movieClient.attachArtwork(any(), any()) } returns null
    }

    private fun movieIdMessage(id: String = movieId.toString()) = uploadFileRequest { movieId = id }

    private fun chunk(text: String) = uploadFileRequest { chunk = ByteString.copyFromUtf8(text) }

    private val complete = uploadFileRequest { complete = true }

    // The one field violation a rejected upload carries, as field → message.
    private suspend fun rejectionOf(vararg messages: UploadFileRequest): Pair<String, String> {
        val error = assertFailsWith<StatusException> { controller.uploadFile(flowOf(*messages)) }
        assertEquals(Status.Code.INVALID_ARGUMENT, error.status.code)
        val violation = StatusProto.fromThrowable(error)!!.detailsList.single()
            .unpack(BadRequest::class.java).fieldViolationsList.single()
        return violation.field to violation.description
    }

    private fun filesOnDisk(): List<Path> = Files.walk(tempDir).use { paths -> paths.filter(Files::isRegularFile).toList() }

    @Test
    fun uploadFile() = runTest {
        val response = controller.uploadFile(flowOf(movieIdMessage(), chunk("ima"), chunk("ge"), complete))

        assertTrue(response.storageKey.startsWith("$movieId/"))
        assertEquals(5, storage.size(response.storageKey))
    }

    @Test
    fun malformedMovieId() = runTest {
        assertEquals("movieId", rejectionOf(movieIdMessage("42"), complete).first)
    }

    @Test
    fun noMovieId() = runTest {
        assertEquals("file" to "The first message must be the movie id", rejectionOf(complete))
    }

    @Test
    fun chunkBeforeMovieId() = runTest {
        assertEquals("file" to "The first message must be the movie id", rejectionOf(chunk("image"), movieIdMessage()))
    }

    @Test
    fun twoMovieIds() = runTest {
        assertEquals(
            "file" to "Only the first message can be the movie id",
            rejectionOf(movieIdMessage(), movieIdMessage(), complete),
        )
    }

    @Test
    fun uploadWithoutCompleteLeavesNothing() = runTest {
        assertEquals(
            "file" to "The upload ended before the whole file arrived",
            rejectionOf(movieIdMessage(), chunk("image")),
        )
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
