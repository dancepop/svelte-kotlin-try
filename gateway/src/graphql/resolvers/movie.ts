import { unary } from "../../grpc/clients.js";
import { nullIfNotFound } from "../../grpc/errors.js";
import { uploadFile } from "../../media/upload.js";
import type { Resolvers } from "../generated/resolvers.js";
import { mediaUrl, toMovie, withoutNulls } from "../mappers.js";

export const movieResolvers: Resolvers = {
  Query: {
    movies: async (_, args, { clients }) => {
      const { search, ...filters } = withoutNulls(args);
      const { movies, totalCount } = await unary(clients.movie, "listMovies", { query: search, ...filters });
      return { items: movies.map(toMovie), totalCount };
    },
    movie: (_, { id }, { clients }) =>
      nullIfNotFound(async () => toMovie(await unary(clients.movie, "getMovie", { id }))),
  },

  Mutation: {
    createMovie: async (_, { input }, { clients }) =>
      toMovie(await unary(clients.movie, "createMovie", { fields: withoutNulls(input) })),
    updateMovie: async (_, { id, input }, { clients }) =>
      toMovie(await unary(clients.movie, "updateMovie", { id, fields: withoutNulls(input) })),
    deleteMovie: async (_, { id }, { clients }) => {
      await unary(clients.movie, "deleteMovie", { id });
      return true;
    },
    uploadMovieArtwork: async (_, { movieId, file }, { clients }) => {
      const { createReadStream } = await file;
      const { storageKey } = await uploadFile(clients.media, movieId, createReadStream());
      return mediaUrl(storageKey);
    },
  },

  // Credits need a join, so they're only fetched when a query asks for them.
  Movie: {
    credits: (movie, _, { loaders }) => loaders.creditsByMovie.load(movie.id),
  },
};
