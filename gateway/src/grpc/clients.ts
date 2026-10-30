import * as grpc from "@grpc/grpc-js";
import * as protoLoader from "@grpc/proto-loader";
import path from "node:path";
import type { GrpcServiceConfig } from "../config.js";

/** Loads the service's .proto at runtime (no codegen) and creates a client for it. */
export function createGrpcClient(cfg: GrpcServiceConfig): grpc.Client {
  const definition = protoLoader.loadSync(path.join(cfg.protoDir, cfg.protoFile), {
    includeDirs: [cfg.protoDir],
    keepCase: true,
    defaults: true,
  });
  const ClientConstructor = cfg.serviceName
    .split(".")
    .reduce<unknown>((node, key) => (node as grpc.GrpcObject)?.[key], grpc.loadPackageDefinition(definition));

  if (typeof ClientConstructor !== "function") {
    throw new Error(`Service ${cfg.serviceName} not found in ${cfg.protoFile}`);
  }
  return new (ClientConstructor as grpc.ServiceClientConstructor)(cfg.address, grpc.credentials.createInsecure());
}
