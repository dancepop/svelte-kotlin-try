export interface GrpcServiceConfig {
  address: string;
  /** Paths inside the backend proto modules: one file per gRPC service. */
  protoFiles: string[];
}

export const config = {
  port: Number(process.env.PORT ?? 4000),
  corsOrigins: (process.env.CORS_ORIGINS ?? "http://localhost:5173").split(","),
  publicUrl: process.env.PUBLIC_URL ?? "http://localhost:4000",
  services: {
    movie: {
      address: process.env.MOVIE_SERVICE_ADDR ?? "localhost:9091",
      protoFiles: ["movie/MovieService.proto", "movie/PersonService.proto", "movie/CreditService.proto"],
    },
    media: {
      address: process.env.MEDIA_SERVICE_ADDR ?? "localhost:9092",
      protoFiles: ["media/MediaService.proto"],
    },
  } satisfies Record<string, GrpcServiceConfig>,
};
