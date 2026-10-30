package com.backend.movie.controller

import com.backend.movie.api.v1.movie
import com.backend.movie.api.v1.movieCredit
import com.backend.movie.api.v1.movieMedia
import com.backend.movie.api.v1.person
import com.backend.movie.api.v1.personCredit
import com.backend.movie.entities.Movie
import com.backend.movie.entities.MovieMedia
import com.backend.movie.entities.Person
import com.backend.movie.enums.CreditType
import com.backend.movie.enums.MediaKind
import com.backend.movie.enums.MovieStatus
import com.backend.movie.exception.FieldError
import com.backend.movie.exception.ValidationException
import com.backend.movie.model.MovieCreditRow
import com.backend.movie.model.MovieWithMedia
import com.backend.movie.model.PersonCreditRow
import com.google.protobuf.Timestamp
import java.time.Instant
import java.util.UUID
import com.backend.movie.api.v1.CreditType as ProtoCreditType
import com.backend.movie.api.v1.MediaKind as ProtoMediaKind
import com.backend.movie.api.v1.Movie as ProtoMovie
import com.backend.movie.api.v1.MovieCredit as ProtoMovieCredit
import com.backend.movie.api.v1.MovieMedia as ProtoMovieMedia
import com.backend.movie.api.v1.MovieStatus as ProtoMovieStatus
import com.backend.movie.api.v1.Person as ProtoPerson
import com.backend.movie.api.v1.PersonCredit as ProtoPersonCredit

// Proto → Kotlin. A bad value is the caller's mistake, so it's a validation error.

internal fun parseUuid(value: String, field: String): UUID = try {
    UUID.fromString(value)
} catch (_: IllegalArgumentException) {
    throw invalid(field, "Not a valid id")
}

internal fun ProtoMovieStatus.toEnum(): MovieStatus = when (this) {
    ProtoMovieStatus.DRAFT -> MovieStatus.DRAFT
    ProtoMovieStatus.PUBLISHED -> MovieStatus.PUBLISHED
    else -> throw invalid("status", "Status is required")
}

internal fun ProtoMediaKind.toEnum(): MediaKind = when (this) {
    ProtoMediaKind.ARTWORK -> MediaKind.ARTWORK
    ProtoMediaKind.PREVIEW -> MediaKind.PREVIEW
    ProtoMediaKind.FULL -> MediaKind.FULL
    else -> throw invalid("kind", "Kind is required")
}

internal fun ProtoCreditType.toEnum(): CreditType = when (this) {
    ProtoCreditType.CAST -> CreditType.CAST
    ProtoCreditType.CREATOR -> CreditType.CREATOR
    else -> throw invalid("type", "Type is required")
}

private fun invalid(field: String, message: String) = ValidationException(listOf(FieldError(field, message)))

// Kotlin → proto.

internal fun MovieWithMedia.toProto(): ProtoMovie {
    val entity = this
    return movie {
        id = entity.movie.id.toString()
        title = entity.movie.title
        entity.movie.synopsis?.let { synopsis = it }
        entity.movie.releaseDate?.let { releaseDate = it.toString() }
        entity.movie.runtimeMinutes?.let { runtimeMinutes = it }
        status = entity.movie.status.toProto()
        media += entity.media.map { it.toProto() }
        version = entity.movie.version!!
        createdAt = entity.movie.createdAt.toProto()
        updatedAt = entity.movie.updatedAt.toProto()
    }
}

internal fun Person.toProto(): ProtoPerson {
    val entity = this
    return person {
        id = entity.id.toString()
        name = entity.name
        entity.bio?.let { bio = it }
        version = entity.version!!
        createdAt = entity.createdAt.toProto()
        updatedAt = entity.updatedAt.toProto()
    }
}

internal fun MovieCreditRow.toProto(): ProtoMovieCredit {
    val row = this
    return movieCredit {
        movieId = row.movieId.toString()
        personId = row.personId.toString()
        name = row.name
        type = row.creditType.toProto()
    }
}

internal fun PersonCreditRow.toProto(): ProtoPersonCredit {
    val row = this
    return personCredit {
        personId = row.personId.toString()
        movieId = row.movieId.toString()
        title = row.title
        status = row.status.toProto()
        type = row.creditType.toProto()
    }
}

private fun MovieMedia.toProto(): ProtoMovieMedia {
    val entity = this
    return movieMedia {
        kind = entity.id.kind.toProto()
        storageKey = entity.storageKey
        contentType = entity.contentType
        sizeBytes = entity.sizeBytes
        entity.durationSeconds?.let { durationSeconds = it }
    }
}

private fun MovieStatus.toProto(): ProtoMovieStatus = when (this) {
    MovieStatus.DRAFT -> ProtoMovieStatus.DRAFT
    MovieStatus.PUBLISHED -> ProtoMovieStatus.PUBLISHED
}

private fun MediaKind.toProto(): ProtoMediaKind = when (this) {
    MediaKind.ARTWORK -> ProtoMediaKind.ARTWORK
    MediaKind.PREVIEW -> ProtoMediaKind.PREVIEW
    MediaKind.FULL -> ProtoMediaKind.FULL
}

private fun CreditType.toProto(): ProtoCreditType = when (this) {
    CreditType.CAST -> ProtoCreditType.CAST
    CreditType.CREATOR -> ProtoCreditType.CREATOR
}

private fun Instant.toProto(): Timestamp =
    Timestamp.newBuilder().setSeconds(epochSecond).setNanos(nano).build()
