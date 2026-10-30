import path from "node:path";
import { fileURLToPath } from "node:url";

// gateway/src (or gateway/dist) -> repo root
const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");

export interface GrpcServiceConfig {
  name: string;
  address: string;
  /** Folder the service team exports its .proto files from (its -api module). */
  protoDir: string;
  protoFile: string;
  /** Fully qualified gRPC service name, e.g. "movie.v1.MovieService". */
  serviceName: string;
}

export const config = {
  port: Number(process.env.PORT ?? 4000),
  corsOrigins: (process.env.CORS_ORIGINS ?? "http://localhost:5173,http://localhost:3000").split(","),
  services: {
    movie: {
      name: "movie-service",
      address: process.env.MOVIE_SERVICE_ADDR ?? "localhost:9091",
      protoDir: process.env.MOVIE_PROTO_DIR ?? path.join(repoRoot, "backend/movie/movie-api/src/main/proto"),
      protoFile: "movie/v1/movie_service.proto",
      serviceName: "movie.v1.MovieService",
    },
    media: {
      name: "media-service",
      address: process.env.MEDIA_SERVICE_ADDR ?? "localhost:9092",
      protoDir: process.env.MEDIA_PROTO_DIR ?? path.join(repoRoot, "backend/media/media-api/src/main/proto"),
      protoFile: "media/v1/media_service.proto",
      serviceName: "media.v1.MediaService",
    },
  } satisfies Record<string, GrpcServiceConfig>,
};
