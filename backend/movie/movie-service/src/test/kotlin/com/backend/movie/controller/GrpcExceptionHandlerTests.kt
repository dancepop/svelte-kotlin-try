package com.backend.movie.controller

import com.backend.movie.api.getMovieRequest
import com.backend.movie.exception.AlreadyExistsException
import com.backend.movie.exception.FieldError
import com.backend.movie.exception.NotFoundException
import com.backend.movie.exception.ValidationException
import com.google.protobuf.Empty
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class GrpcExceptionHandlerTests {

    @Test
    fun validationExceptionIsInvalidArgument() {
        val error = toStatusException(
            ValidationException(
                listOf(
                    FieldError("title", "Title is required"),
                    FieldError("runtimeMinutes", "Runtime must be 1 to 1000 minutes"),
                ),
            ),
        )

        assertEquals(Status.Code.INVALID_ARGUMENT, error.status.code)
        assertEquals(
            mapOf("title" to "Title is required", "runtimeMinutes" to "Runtime must be 1 to 1000 minutes"),
            fieldViolationsOf(error),
        )
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
