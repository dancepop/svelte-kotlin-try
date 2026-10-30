import { GraphQLError } from "graphql";
import { GraphQLUpload, type FileUpload } from "graphql-upload-minimal";
import { unary } from "./grpc/clients.js";
import type { CreditType, ListMoviesResponse, ListPeopleResponse, Movie, Person } from "./grpc/types.js";
import type { Context } from "./loaders.js";
import { mediaUrl, toMovie, toPerson } from "./mappers.js";
import { uploadFile } from "./upload.js";

interface MovieListArgs {
  search?: string | null;
  castName?: string | null;
  creatorName?: string | null;
  page: number;
  pageSize: number;
}
interface PersonListArgs {
  search?: string | null;
  page: number;
  pageSize: number;
}
interface IdArgs {
  id: string;
}
interface MovieInput {
  title: string;
  synopsis?: string | null;
  releaseDate?: string | null;
  runtimeMinutes?: number | null;
}
interface UploadArgs {
  movieId: string;
  file: Promise<FileUpload>;
}
interface CreditArgs {
  movieId: string;
  personId: string;
  type: CreditType;
}

// Each resolver below makes one gRPC call. The rules are in the services; the gateway only translates.
export const resolvers = {
  Upload: GraphQLUpload,

  Query: {
    movies: async (_: unknown, args: MovieListArgs, { clients }: Context) => {
      const { movies, totalCount } = await unary<ListMoviesResponse>(clients.movie, "listMovies", {
        query: args.search,
        castName: args.castName,
        creatorName: args.creatorName,
        page: args.page,
        pageSize: args.pageSize,
      });
      return { items: movies.map(toMovie), totalCount };
    },
    movie: async (_: unknown, { id }: IdArgs, { clients }: Context) =>
      nullIfNotFound(async () => toMovie(await unary<Movie>(clients.movie, "getMovie", { id }))),
    people: async (_: unknown, args: PersonListArgs, { clients }: Context) => {
      const { people, totalCount } = await unary<ListPeopleResponse>(clients.person, "listPeople", {
        query: args.search,
        page: args.page,
        pageSize: args.pageSize,
      });
      return { items: people.map(toPerson), totalCount };
    },
    person: async (_: unknown, { id }: IdArgs, { clients }: Context) =>
      nullIfNotFound(async () => toPerson(await unary<Person>(clients.person, "getPerson", { id }))),
  },

  Mutation: {
    createMovie: async (_: unknown, { input }: { input: MovieInput }, { clients }: Context) =>
      toMovie(await unary<Movie>(clients.movie, "createMovie", { fields: input })),
    updateMovie: async (_: unknown, { id, input }: IdArgs & { input: MovieInput }, { clients }: Context) =>
      toMovie(await unary<Movie>(clients.movie, "updateMovie", { id, fields: input })),
    deleteMovie: async (_: unknown, { id }: IdArgs, { clients }: Context) => {
      await unary(clients.movie, "deleteMovie", { id });
      return true;
    },
    uploadMovieArtwork: async (_: unknown, { movieId, file }: UploadArgs, { clients }: Context) => {
      const { createReadStream } = await file;
      const { storageKey } = await uploadFile(clients.media, movieId, createReadStream());
      return mediaUrl(storageKey);
    },
    addCredit: async (_: unknown, args: CreditArgs, { clients }: Context) => {
      await unary(clients.credit, "addCredit", args);
      return true;
    },
    removeCredit: async (_: unknown, args: CreditArgs, { clients }: Context) => {
      await unary(clients.credit, "removeCredit", args);
      return true;
    },
  },

  // The only nested fields with resolvers: they need a join, so they run only when a query asks for them.
  Movie: {
    credits: (movie: { id: string }, _: unknown, { loaders }: Context) => loaders.creditsByMovie.load(movie.id),
  },
  Person: {
    credits: (person: { id: string }, _: unknown, { loaders }: Context) => loaders.creditsByPerson.load(person.id),
  },
};

async function nullIfNotFound<T>(load: () => Promise<T>): Promise<T | null> {
  try {
    return await load();
  } catch (error) {
    if (error instanceof GraphQLError && error.extensions.code === "NOT_FOUND") return null;
    throw error;
  }
}
