package com.backend.movie.controller

import com.backend.movie.api.AttachArtworkRequest
import com.backend.movie.api.AttachArtworkResponse
import com.backend.movie.api.CreateMovieRequest
import com.backend.movie.api.DeleteMovieRequest
import com.backend.movie.api.GetMovieRequest
import com.backend.movie.api.ListMoviesRequest
import com.backend.movie.api.ListMoviesResponse
import com.backend.movie.api.Movie
import com.backend.movie.api.MovieFields
import com.backend.movie.api.MovieServiceGrpcKt
import com.backend.movie.api.UpdateMovieRequest
import com.backend.movie.api.attachArtworkResponse
import com.backend.movie.api.listMoviesResponse
import com.backend.movie.model.MovieFilter
import com.backend.movie.service.MovieService
import com.google.protobuf.Empty
import org.springframework.grpc.server.service.GrpcService

/**
 * gRPC entry point for movies (the gRPC equivalent of a REST controller). Rejects invalid values and converts between
 * proto and Kotlin types; the business logic is in [MovieService]. Spring Boot registers every BindableService bean with the gRPC server.
 */
@GrpcService
class MovieGrpcController(
    private val movieService: MovieService,
) : MovieServiceGrpcKt.MovieServiceCoroutineImplBase() {

    override suspend fun listMovies(request: ListMoviesRequest): ListMoviesResponse = rpc {
        requireValid(pagingErrors(request.page, request.pageSize))
        val filter = MovieFilter(
            query = request.query,
            castName = request.castName,
            creatorName = request.creatorName,
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
        fields.requireValidFields()
        movieService.createMovie(fields.title, fields.synopsisOrNull(), fields.releaseDateOrNull(), fields.runtimeOrNull())
            .toProto()
    }

    override suspend fun updateMovie(request: UpdateMovieRequest): Movie = rpc {
        val fields = request.fields
        fields.requireValidFields()
        movieService.updateMovie(
            parseUuid(request.id, "id"),
            fields.title,
            fields.synopsisOrNull(),
            fields.releaseDateOrNull(),
            fields.runtimeOrNull(),
        ).toProto()
    }

    override suspend fun deleteMovie(request: DeleteMovieRequest): Empty = rpc {
        movieService.deleteMovie(parseUuid(request.id, "id"))
        Empty.getDefaultInstance()
    }

    override suspend fun attachArtwork(request: AttachArtworkRequest): AttachArtworkResponse = rpc {
        val replaced = movieService.attachArtwork(parseUuid(request.movieId, "movieId"), request.storageKey)
        attachArtworkResponse { replaced?.let { replacedStorageKey = it } }
    }

    private fun MovieFields.requireValidFields() {
        requireValid(movieFieldErrors(title, synopsisOrNull(), releaseDateOrNull(), runtimeOrNull()))
    }

    private fun MovieFields.synopsisOrNull(): String? = if (hasSynopsis()) synopsis else null

    private fun MovieFields.releaseDateOrNull(): String? = if (hasReleaseDate()) releaseDate else null

    private fun MovieFields.runtimeOrNull(): Int? = if (hasRuntimeMinutes()) runtimeMinutes else null
}
