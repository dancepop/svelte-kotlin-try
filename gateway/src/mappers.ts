import { config } from "./config.js";
import type { Movie, Person, Timestamp } from "./grpc/types.js";

/** A gRPC movie in the shape of the GraphQL `Movie`. Its `credits` come from a resolver. */
export function toMovie(movie: Movie) {
  return {
    ...movie,
    artworkUrl: movie.artworkKey ? mediaUrl(movie.artworkKey) : null,
    createdAt: toIsoString(movie.createdAt),
    updatedAt: toIsoString(movie.updatedAt),
  };
}

export function toPerson(person: Person) {
  return { ...person, createdAt: toIsoString(person.createdAt), updatedAt: toIsoString(person.updatedAt) };
}

export function mediaUrl(storageKey: string): string {
  return `${config.publicUrl}/media/${storageKey}`;
}

function toIsoString(timestamp: Timestamp): string {
  return new Date(timestamp.seconds * 1000 + timestamp.nanos / 1_000_000).toISOString();
}
