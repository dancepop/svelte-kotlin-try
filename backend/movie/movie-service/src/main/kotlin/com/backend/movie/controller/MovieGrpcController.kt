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
import java.time.LocalDate
import java.util.UUID

/**
 * gRPC entry point for movies (the gRPC equivalent of a REST controller). [rpc] checks each request against the rules
 * in the .proto first. Converts between proto and Kotlin types; the business logic is in [MovieService]. Spring Boot registers every BindableService bean with the gRPC server.
 */
@GrpcService
class MovieGrpcController(
    private val movieService: MovieService,
) : MovieServiceGrpcKt.MovieServiceCoroutineImplBase() {

    override suspend fun listMovies(request: ListMoviesRequest): ListMoviesResponse = rpc(request) {
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

    override suspend fun getMovie(request: GetMovieRequest): Movie = rpc(request) {
        movieService.getMovie(UUID.fromString(request.id)).toProto()
    }

    override suspend fun createMovie(request: CreateMovieRequest): Movie = rpc(request) {
        val fields = request.fields
        movieService.createMovie(fields.title, fields.synopsisOrNull(), fields.releaseDateOrNull(), fields.runtimeOrNull())
            .toProto()
    }

    override suspend fun updateMovie(request: UpdateMovieRequest): Movie = rpc(request) {
        val fields = request.fields
        movieService.updateMovie(
            UUID.fromString(request.id),
            fields.title,
            fields.synopsisOrNull(),
            fields.releaseDateOrNull(),
            fields.runtimeOrNull(),
        ).toProto()
    }

    override suspend fun deleteMovie(request: DeleteMovieRequest): Empty = rpc(request) {
        movieService.deleteMovie(UUID.fromString(request.id))
        Empty.getDefaultInstance()
    }

    override suspend fun attachArtwork(request: AttachArtworkRequest): AttachArtworkResponse = rpc(request) {
        val replaced = movieService.attachArtwork(UUID.fromString(request.movieId), request.storageKey)
        attachArtworkResponse { replaced?.let { replacedStorageKey = it } }
    }

    private fun MovieFields.synopsisOrNull(): String? = if (hasSynopsis()) synopsis else null

    private fun MovieFields.releaseDateOrNull(): LocalDate? = if (hasReleaseDate()) parseDate(releaseDate) else null

    private fun MovieFields.runtimeOrNull(): Int? = if (hasRuntimeMinutes()) runtimeMinutes else null
}
