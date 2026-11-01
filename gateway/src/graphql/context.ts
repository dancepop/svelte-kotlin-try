import type { Clients } from "../grpc/clients.js";
import { createLoaders } from "./loaders.js";

/** What every resolver receives as its third argument. */
export interface Context {
  clients: Clients;
  loaders: ReturnType<typeof createLoaders>;
}

/** Built per request, so cached loader results are never shared between requests. */
export function createContext(clients: Clients): Context {
  return { clients, loaders: createLoaders(clients) };
}
