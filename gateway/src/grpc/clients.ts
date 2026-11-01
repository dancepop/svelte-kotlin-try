import * as grpc from "@grpc/grpc-js";
import * as protoLoader from "@grpc/proto-loader";
import path from "node:path";
import { fileURLToPath } from "node:url";
import type { GrpcServiceConfig } from "../config.js";
import { toGraphQLError } from "../errors.js";

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

export type GrpcClient = grpc.Client & Record<string, any>;

export interface Clients {
  movie: GrpcClient;
  person: GrpcClient;
  credit: GrpcClient;
  media: GrpcClient;
}

export function createClients(services: { movie: GrpcServiceConfig; media: GrpcServiceConfig }): Clients {
  const movieApi = loadProto(services.movie);
  const mediaApi = loadProto(services.media);
  return {
    movie: createClient(movieApi, "movie", "MovieService", services.movie.address),
    person: createClient(movieApi, "movie", "PersonService", services.movie.address),
    credit: createClient(movieApi, "movie", "CreditService", services.movie.address),
    media: createClient(mediaApi, "media", "MediaService", services.media.address),
  };
}

/** Calls a one-request, one-response method. A failure rejects with the matching GraphQL error. */
export function unary<Response>(client: GrpcClient, method: string, request: unknown): Promise<Response> {
  return new Promise((resolve, reject) => {
    // waitForReady: if a service has just restarted, wait for the connection until the deadline.
    const options = { deadline: Date.now() + DEADLINE_MS, waitForReady: true };
    client[method](request, options, (error: grpc.ServiceError | null, response: Response) => {
      if (error) reject(toGraphQLError(error));
      else resolve(response);
    });
  });
}

/** Resolves once the client has a connection, and rejects if it can't get one by the deadline. */
export function connected(client: GrpcClient): Promise<void> {
  return new Promise((resolve, reject) => {
    client.waitForReady(Date.now() + DEADLINE_MS, (error?: Error) =>
      error ? reject(Object.assign(error, { code: grpc.status.UNAVAILABLE })) : resolve(),
    );
  });
}

/** Loads the service's .proto at runtime (no codegen). */
function loadProto(cfg: GrpcServiceConfig): grpc.GrpcObject {
  const definition = protoLoader.loadSync(cfg.protoFiles, {
    includeDirs: PROTO_DIRS,
    keepCase: false, // total_count becomes totalCount
    enums: String, // "CAST" rather than 1
    longs: Number, // JS numbers are exact up to 9 PB, far above any count or file size here
    defaults: true, // Unset optional fields stay undefined; everything else gets its default
  });
  return grpc.loadPackageDefinition(definition);
}

/** pkg is the .proto's `package` line, service its `service` name. */
function createClient(api: grpc.GrpcObject, pkg: string, service: string, address: string): GrpcClient {
  const Client = (api[pkg] as grpc.GrpcObject)[service] as grpc.ServiceClientConstructor;
  return new Client(address, grpc.credentials.createInsecure());
}
