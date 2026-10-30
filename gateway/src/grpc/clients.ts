import * as grpc from "@grpc/grpc-js";
import * as protoLoader from "@grpc/proto-loader";
import path from "node:path";
import { fileURLToPath } from "node:url";
import type { GrpcServiceConfig } from "../config.js";
import { toGraphQLError } from "../errors.js";

// Physical copies of the contracts in each backend -api module, so they must be re-copied when a contract changes.
// With separate teams this should be a package install: each -api module publishes a versioned package.
const PROTO_DIR = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../proto"); // from src/grpc or dist/grpc

const DEADLINE_MS = 5_000;

/** UploadFile and ReadFile last as long as the transfer. This allows for 5 MB on a very slow connection. */
export const TRANSFER_DEADLINE_MS = 5 * 60 * 1000;

/** A client whose methods come from the .proto at runtime, so TypeScript can't know them. */
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
    movie: createClient(movieApi, "movie.MovieService", services.movie.address),
    person: createClient(movieApi, "movie.PersonService", services.movie.address),
    credit: createClient(movieApi, "movie.CreditService", services.movie.address),
    media: createClient(mediaApi, "media.MediaService", services.media.address),
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
  const definition = protoLoader.loadSync(cfg.protoFile, {
    includeDirs: [PROTO_DIR],
    keepCase: false, // total_count becomes totalCount
    enums: String, // "CAST" rather than 1
    longs: Number, // JS numbers are exact up to 9 PB, far above any count or file size here
    defaults: true, // Unset optional fields stay undefined; everything else gets its default
  });
  return grpc.loadPackageDefinition(definition);
}

function createClient(api: grpc.GrpcObject, serviceName: string, address: string): GrpcClient {
  const ClientConstructor = serviceName.split(".").reduce<unknown>((node, key) => (node as grpc.GrpcObject)?.[key], api);
  if (typeof ClientConstructor !== "function") {
    throw new Error(`Service ${serviceName} not found`);
  }
  return new (ClientConstructor as grpc.ServiceClientConstructor)(address, grpc.credentials.createInsecure());
}
