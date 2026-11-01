import { config } from "../config.js";
import type { Timestamp__Output } from "../grpc/generated/google/protobuf/Timestamp.js";
import type { Movie__Output } from "../grpc/generated/movie/Movie.js";
import type { Person__Output } from "../grpc/generated/movie/Person.js";

/** A gRPC movie in the shape of the GraphQL `Movie`. Its `credits` come from a resolver. */
export function toMovie(movie: Movie__Output) {
  return {
    ...movie,
    artworkUrl: movie.artworkKey ? mediaUrl(movie.artworkKey) : null,
    createdAt: toIsoString(movie.createdAt),
    updatedAt: toIsoString(movie.updatedAt),
  };
}
export type MovieModel = ReturnType<typeof toMovie>;

export function toPerson(person: Person__Output) {
  return { ...person, createdAt: toIsoString(person.createdAt), updatedAt: toIsoString(person.updatedAt) };
}
export type PersonModel = ReturnType<typeof toPerson>;

export function mediaUrl(storageKey: string): string {
  return `${config.publicUrl}/media/${storageKey}`;
}

/** GraphQL gives an omitted optional argument as null; a gRPC request leaves it out instead. */
export function withoutNulls<T extends object>(value: T): { [K in keyof T]: Exclude<T[K], null> } {
  return Object.fromEntries(Object.entries(value).filter(([, v]) => v !== null)) as { [K in keyof T]: Exclude<T[K], null> };
}

// The services always set both timestamps; proto3 still types every message field as possibly missing.
function toIsoString(timestamp: Timestamp__Output | null): string {
  const { seconds, nanos } = timestamp!;
  return new Date(seconds * 1000 + nanos / 1_000_000).toISOString();
}
