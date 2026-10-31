package com.backend.movie.controller

import com.backend.movie.entities.Movie
import com.backend.movie.enums.CreditType
import com.backend.movie.exception.FieldError
import com.backend.movie.exception.ValidationException
import com.backend.movie.model.MovieCreditRow
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.backend.movie.api.CreditType as ProtoCreditType

class ProtoMappersTests {

    @Test
    fun parseDate() {
        assertEquals(LocalDate.of(2024, 2, 29), parseDate("2024-02-29"))
    }

    @Test
    fun parseDateThatDoesNotExist() {
        val error = assertFailsWith<ValidationException> { parseDate("2025-02-30") }

        assertEquals(
            listOf(FieldError("releaseDate", "Release date must be a real date in the format YYYY-MM-DD")),
            error.fieldErrors,
        )
    }

    @Test
    fun creditTypes() {
        assertEquals(CreditType.CAST, ProtoCreditType.CAST.toEnum())
        assertEquals(CreditType.CREATOR, ProtoCreditType.CREATOR.toEnum())

        val row = MovieCreditRow(UUID.randomUUID(), UUID.randomUUID(), "Michael Mann", CreditType.CREATOR)
        assertEquals(ProtoCreditType.CREATOR, row.toProto().type)
    }

    @Test
    fun movieWithEmptyOptionalFields() {
        val proto = Movie(id = UUID.randomUUID(), title = "Heat").toProto()

        assertFalse(proto.hasSynopsis())
        assertFalse(proto.hasReleaseDate())
        assertFalse(proto.hasRuntimeMinutes())
        assertFalse(proto.hasArtworkKey())
    }

    @Test
    fun movieWithFilledOptionalFields() {
        val movie = Movie(
            id = UUID.randomUUID(),
            title = "Heat",
            synopsis = "A heist",
            releaseDate = LocalDate.of(1995, 12, 15),
            runtimeMinutes = 170,
            artworkKey = "a/b.jpg",
        )

        val proto = movie.toProto()

        assertTrue(proto.hasSynopsis())
        assertEquals("1995-12-15", proto.releaseDate)
        assertEquals(170, proto.runtimeMinutes)
        assertEquals("a/b.jpg", proto.artworkKey)
    }
}
