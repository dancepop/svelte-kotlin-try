package com.backend.movie.controller

import com.backend.movie.api.getMovieRequest
import com.backend.movie.exception.AlreadyExistsException
import com.backend.movie.exception.NotFoundException
import com.backend.movie.exception.ValidationException
import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.springframework.dao.OptimisticLockingFailureException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class GrpcExceptionHandlerTests {

    @Test
    fun validationExceptionIsInvalidArgument() {
        val error = toStatusException(ValidationException("Title is required; Runtime must be 1 to 1000 minutes"))

        assertEquals(Status.Code.INVALID_ARGUMENT, error.status.code)
        assertEquals("Title is required; Runtime must be 1 to 1000 minutes", error.status.description)
    }

    @Test
    fun notFoundExceptionIsNotFound() {
        val error = toStatusException(NotFoundException("This movie no longer exists"))

        assertEquals(Status.Code.NOT_FOUND, error.status.code)
        assertEquals("This movie no longer exists", error.status.description)
    }

    @Test
    fun alreadyExistsExceptionIsAlreadyExists() {
        val error = toStatusException(AlreadyExistsException("Already credited as cast on this movie"))

        assertEquals(Status.Code.ALREADY_EXISTS, error.status.code)
        assertEquals("Already credited as cast on this movie", error.status.description)
    }

    @Test
    fun versionConflictIsAborted() {
        val error = toStatusException(OptimisticLockingFailureException("Failed to update versioned entity"))

        assertEquals(Status.Code.ABORTED, error.status.code)
        assertEquals("Someone else changed this at the same time. Reload and try again.", error.status.description)
    }

    @Test
    fun otherExceptionIsInternal() {
        val error = toStatusException(IllegalStateException("password=secret"))

        assertEquals(Status.Code.INTERNAL, error.status.code)
        assertEquals("Something went wrong", error.status.description)
    }

    @Test
    fun cancellationStaysCancellation() = runTest {
        assertFailsWith<CancellationException> {
            rpc(Empty.getDefaultInstance()) { throw CancellationException("The client went away") }
        }
    }

    @Test
    fun invalidRequestSkipsBlock() = runTest {
        var ran = false

        val error = assertFailsWith<StatusException> { rpc(getMovieRequest { id = "not-a-uuid" }) { ran = true } }

        assertEquals(Status.Code.INVALID_ARGUMENT, error.status.code)
        assertFalse(ran)
    }
}
