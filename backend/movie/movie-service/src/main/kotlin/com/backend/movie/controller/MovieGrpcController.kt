package com.backend.movie.controller

import com.backend.movie.api.v1.AttachMediaRequest
import com.backend.movie.api.v1.AttachMediaResponse
import com.backend.movie.api.v1.ChangeMovieStatusRequest
import com.backend.movie.api.v1.CreateMovieRequest
import com.backend.movie.api.v1.DeleteMovieRequest
import com.backend.movie.api.v1.GetMovieRequest
import com.backend.movie.api.v1.ListMoviesRequest
import com.backend.movie.api.v1.ListMoviesResponse
import com.backend.movie.api.v1.Movie
import com.backend.movie.api.v1.MovieFields
import com.backend.movie.api.v1.MovieServiceGrpcKt
import com.backend.movie.api.v1.RemoveMediaRequest
import com.backend.movie.api.v1.UpdateMovieRequest
import com.backend.movie.api.v1.attachMediaResponse
import com.backend.movie.api.v1.listMoviesResponse
import com.backend.movie.model.MovieFilter
import com.backend.movie.service.MovieService
import com.google.protobuf.Empty
import org.springframework.grpc.server.service.GrpcService

/**
 * gRPC entry point for movies (the gRPC equivalent of a REST controller). Only converts between proto and
 * Kotlin types; the rules are in [MovieService]. Spring Boot registers every BindableService bean with the gRPC server.
 */
@GrpcService
class MovieGrpcController(
    private val movieService: MovieService,
) : MovieServiceGrpcKt.MovieServiceCoroutineImplBase() {

    override suspend fun listMovies(request: ListMoviesRequest): ListMoviesResponse = rpc {
        val filter = MovieFilter(
            query = request.query,
            castName = request.castName,
            creatorName = request.creatorName,
            status = request.status.toEnum(),
            page = request.page,
            pageSize = request.pageSize,
        )
        val page = movieService.listMovies(filter)
        listMoviesResponse {
            movies += page.items.map { it.toProto() }
            totalCount = page.totalCount
        }
    }

    override suspend fun getMovie(request: GetMovieRequest): Movie = rpc {
        movieService.getMovie(parseUuid(request.id, "id")).toProto()
    }

    override suspend fun createMovie(request: CreateMovieRequest): Movie = rpc {
        val fields = request.fields
        movieService.createMovie(fields.title, fields.synopsisOrNull(), fields.releaseDateOrNull(), fields.runtimeOrNull())
            .toProto()
    }

    override suspend fun updateMovie(request: UpdateMovieRequest): Movie = rpc {
        val fields = request.fields
        movieService.updateMovie(
            parseUuid(request.id, "id"),
            request.version,
            fields.title,
            fields.synopsisOrNull(),
            fields.releaseDateOrNull(),
            fields.runtimeOrNull(),
        ).toProto()
    }

    override suspend fun publishMovie(request: ChangeMovieStatusRequest): Movie = rpc {
        movieService.publishMovie(parseUuid(request.id, "id"), request.version).toProto()
    }

    override suspend fun unpublishMovie(request: ChangeMovieStatusRequest): Movie = rpc {
        movieService.unpublishMovie(parseUuid(request.id, "id"), request.version).toProto()
    }

    override suspend fun deleteMovie(request: DeleteMovieRequest): Empty = rpc {
        movieService.deleteMovie(parseUuid(request.id, "id"), request.version)
        Empty.getDefaultInstance()
    }

    override suspend fun attachMedia(request: AttachMediaRequest): AttachMediaResponse = rpc {
        val replaced = movieService.attachMedia(
            parseUuid(request.movieId, "movieId"),
            request.kind.toEnum(),
            request.storageKey,
            request.contentType,
            request.sizeBytes,
            if (request.hasDurationSeconds()) request.durationSeconds else null,
        )
        attachMediaResponse { replaced?.let { replacedStorageKey = it } }
    }

    override suspend fun removeMedia(request: RemoveMediaRequest): Movie = rpc {
        movieService.removeMedia(parseUuid(request.movieId, "movieId"), request.kind.toEnum()).toProto()
    }

    private fun MovieFields.synopsisOrNull(): String? = if (hasSynopsis()) synopsis else null

    private fun MovieFields.releaseDateOrNull(): String? = if (hasReleaseDate()) releaseDate else null

    private fun MovieFields.runtimeOrNull(): Int? = if (hasRuntimeMinutes()) runtimeMinutes else null
}
