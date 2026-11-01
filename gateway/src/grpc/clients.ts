import * as grpc from "@grpc/grpc-js";
import * as protoLoader from "@grpc/proto-loader";
import path from "node:path";
import { fileURLToPath } from "node:url";
import type { GrpcServiceConfig } from "../config.js";
import { toGraphQLError } from "./errors.js";
import type { ProtoGrpcType as CreditProto } from "./generated/CreditService.js";
import type { ProtoGrpcType as MediaProto } from "./generated/MediaService.js";
import type { ProtoGrpcType as MovieProto } from "./generated/MovieService.js";
import type { ProtoGrpcType as PersonProto } from "./generated/PersonService.js";
import type { MediaServiceClient } from "./generated/media/MediaService.js";
import type { CreditServiceClient } from "./generated/movie/CreditService.js";
import type { MovieServiceClient } from "./generated/movie/MovieService.js";
import type { PersonServiceClient } from "./generated/movie/PersonService.js";

// Maybe would've been better to use Apollo router with federation instead of Apollo server but
// double api controllers on each backend service?
const REPO_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../..");
const PROTO_DIRS = [
  path.join(REPO_ROOT, "backend/movie/movie-proto/src/main/proto"),
  path.join(REPO_ROOT, "backend/media/media-proto/src/main/proto"),
  path.join(REPO_ROOT, "gateway/proto"),
];

const DEADLINE_MS = 5_000;

/** UploadFile and ReadFile last as long as the transfer. This allows for 5 MB on a very slow connection. */
export const TRANSFER_DEADLINE_MS = 5 * 60 * 1000;

export interface Clients {
  movie: MovieServiceClient;
  person: PersonServiceClient;
  credit: CreditServiceClient;
  media: MediaServiceClient;
}

export function createClients(services: { movie: GrpcServiceConfig; media: GrpcServiceConfig }): Clients {
  const movieApi = loadProto<MovieProto & PersonProto & CreditProto>(services.movie);
  const mediaApi = loadProto<MediaProto>(services.media);
  const insecure = grpc.credentials.createInsecure();
  return {
    movie: new movieApi.movie.MovieService(services.movie.address, insecure),
    person: new movieApi.movie.PersonService(services.movie.address, insecure),
    credit: new movieApi.movie.CreditService(services.movie.address, insecure),
    media: new mediaApi.media.MediaService(services.media.address, insecure),
  };
}

// The types of a one-request, one-response method, read from its shortest overload: (request, callback).
type UnaryShape<Req, Res> = (request: Req, callback: grpc.requestCallback<Res>) => grpc.ClientUnaryCall;
type UnaryMethodName<C> = { [K in keyof C]: C[K] extends UnaryShape<any, any> ? K : never }[keyof C];
type RequestOf<F> = F extends UnaryShape<infer Req, any> ? Req : never;
type ResponseOf<F> = F extends UnaryShape<any, infer Res> ? Res : never;

/**
 * Calls a one-request, one-response method, e.g. `unary(clients.movie, "getMovie", { id })`.
 * The method name, request and response are checked against the .proto. A failure rejects with the matching GraphQL error.
 */
export function unary<C extends grpc.Client, M extends UnaryMethodName<C>>(
  client: C,
  method: M,
  request: RequestOf<C[M]>,
): Promise<ResponseOf<C[M]>> {
  type Callback = grpc.requestCallback<ResponseOf<C[M]>>;
  const call = client[method] as (request: RequestOf<C[M]>, options: grpc.CallOptions, callback: Callback) => void;
  return new Promise((resolve, reject) => {
    call.call(client, request, { deadline: Date.now() + DEADLINE_MS }, (error, response) => {
      if (error) reject(toGraphQLError(error));
      else resolve(response!);
    });
  });
}

/** Loads the service's .proto at runtime. The generated types describe what comes back. */
function loadProto<Api>(cfg: GrpcServiceConfig): Api {
  // These options must match the codegen:grpc script in package.json, or the generated types won't fit.
  const definition = protoLoader.loadSync(cfg.protoFiles, {
    includeDirs: PROTO_DIRS,
    keepCase: false, // total_count becomes totalCount
    enums: String, // "CAST" rather than 1
    longs: Number, // JS numbers are exact up to 9 PB, far above any count or file size here
    defaults: true, // Unset optional fields stay undefined; everything else gets its default
  });
  return grpc.loadPackageDefinition(definition) as unknown as Api;
}
