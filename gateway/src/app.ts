import { ApolloServer } from "@apollo/server";
import { ApolloServerPluginDrainHttpServer } from "@apollo/server/plugin/drainHttpServer";
import { expressMiddleware } from "@as-integrations/express5";
import cors from "cors";
import express from "express";
import { graphqlUploadExpress } from "graphql-upload-minimal";
import { readFileSync } from "node:fs";
import http from "node:http";
import { config } from "./config.js";
import { type Context, createContext } from "./graphql/context.js";
import { resolvers } from "./graphql/resolvers/index.js";
import type { Clients } from "./grpc/clients.js";
import { mediaRoute } from "./media/route.js";

// schema.graphql is also the input to codegen:graphql. The build copies it to dist/graphql.
const typeDefs = readFileSync(new URL("./graphql/schema.graphql", import.meta.url), "utf8");

export async function createApp(clients: Clients) {
  const app = express();
  const httpServer = http.createServer(app);

  const apollo = new ApolloServer<Context>({
    typeDefs,
    resolvers,
    plugins: [ApolloServerPluginDrainHttpServer({ httpServer })],
  });
  await apollo.start();

  app.get("/media/*key", mediaRoute(clients.media));
  app.use(
    "/graphql",
    cors({ origin: config.corsOrigins }),
    express.json(),
    graphqlUploadExpress({ maxFiles: 1 }),
    expressMiddleware(apollo, { context: async () => createContext(clients) }),
  );

  return { app, httpServer, apollo };
}
