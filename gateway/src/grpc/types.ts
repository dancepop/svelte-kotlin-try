// The gRPC messages as the proto loader returns them. The .proto files are the source of truth; these are checked by hand.

export type CreditType = "CAST" | "CREATOR";

export interface Timestamp {
  seconds: number;
  nanos: number;
}

export interface Movie {
  id: string;
  title: string;
  synopsis?: string;
  releaseDate?: string;
  runtimeMinutes?: number;
  artworkKey?: string;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}

export interface ListMoviesResponse {
  movies: Movie[];
  totalCount: number;
}

export interface Person {
  id: string;
  name: string;
  bio?: string;
  createdAt: Timestamp;
  updatedAt: Timestamp;
}

export interface ListPeopleResponse {
  people: Person[];
  totalCount: number;
}

export interface MovieCredit {
  movieId: string;
  personId: string;
  name: string;
  type: CreditType;
}

export interface PersonCredit {
  personId: string;
  movieId: string;
  title: string;
  type: CreditType;
}

export interface UploadFileResponse {
  storageKey: string;
}

export interface FileInfo {
  totalSize: number;
}

export interface ReadFileResponse {
  info?: FileInfo;
  chunk?: Uint8Array;
}
