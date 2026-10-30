export interface GrpcServiceConfig {
  address: string;
  /** Path inside gateway/proto. */
  protoFile: string;
}

export const config = {
  port: Number(process.env.PORT ?? 4000),
  corsOrigins: (process.env.CORS_ORIGINS ?? "http://localhost:5173").split(","),
  /** The gateway's address as seen from the browser; media URLs start with it. */
  publicUrl: process.env.PUBLIC_URL ?? "http://localhost:4000",
  services: {
    movie: {
      address: process.env.MOVIE_SERVICE_ADDR ?? "localhost:9091",
      protoFile: "movie/movie_service.proto",
    },
    media: {
      address: process.env.MEDIA_SERVICE_ADDR ?? "localhost:9092",
      protoFile: "media/media_service.proto",
    },
  } satisfies Record<string, GrpcServiceConfig>,
};
