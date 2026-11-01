package com.backend.movie.controller

import com.backend.movie.api.movie
import com.backend.movie.api.movieCredit
import com.backend.movie.api.person
import com.backend.movie.api.personCredit
import com.backend.movie.entities.Movie
import com.backend.movie.entities.Person
import com.backend.movie.enums.CreditType
import com.backend.movie.model.MovieCreditRow
import com.backend.movie.model.PersonCreditRow
import com.google.protobuf.Timestamp
import java.time.Instant
// Rename conflicting types with "Proto" prefix
import com.backend.movie.api.CreditType as ProtoCreditType
import com.backend.movie.api.Movie as ProtoMovie
import com.backend.movie.api.MovieCredit as ProtoMovieCredit
import com.backend.movie.api.Person as ProtoPerson
import com.backend.movie.api.PersonCredit as ProtoPersonCredit

// Used for service to controller return types to convert to proto

internal fun Movie.toProto(): ProtoMovie {
    val entity = this
    return movie {
        id = entity.id.toString()
        title = entity.title
        entity.synopsis?.let { synopsis = it }
        entity.releaseDate?.let { releaseDate = it.toString() }
        entity.runtimeMinutes?.let { runtimeMinutes = it }
        entity.artworkKey?.let { artworkKey = it }
        createdAt = entity.createdAt.toProto()
        updatedAt = entity.updatedAt.toProto()
    }
}

internal fun Person.toProto(): ProtoPerson {
    val entity = this
    return person {
        id = entity.id.toString()
        name = entity.name
        entity.bio?.let { bio = it }
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
        type = row.creditType.toProto()
    }
}

private fun CreditType.toProto(): ProtoCreditType = ProtoCreditType.valueOf(name)

private fun Instant.toProto(): Timestamp =
    Timestamp.newBuilder().setSeconds(epochSecond).setNanos(nano).build()
