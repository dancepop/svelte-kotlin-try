import { ApolloServer } from "@apollo/server";
import { ApolloServerPluginDrainHttpServer } from "@apollo/server/plugin/drainHttpServer";
import { expressMiddleware } from "@as-integrations/express5";
import cors from "cors";
import express from "express";
import { graphqlUploadExpress } from "graphql-upload-minimal";
import http from "node:http";
import type { Clients } from "./grpc/clients.js";
import { type Context, createLoaders } from "./loaders.js";
import { mediaRoute } from "./media-route.js";
import { resolvers } from "./resolvers.js";
import { typeDefs } from "./schema.js";

/** Express hosts Apollo on /graphql; plain HTTP routes (health, artwork) sit next to it. */
export async function createApp(corsOrigins: string[], clients: Clients) {
  const app = express();
  const httpServer = http.createServer(app);

  const apollo = new ApolloServer<Context>({
    typeDefs,
    resolvers,
    plugins: [ApolloServerPluginDrainHttpServer({ httpServer })],
  });
  await apollo.start();

  app.get("/healthz", (_req, res) => {
    res.json({ status: "ok" });
  });
  app.get("/media/*key", mediaRoute(clients.media));
  app.use(
    "/graphql",
    cors({ origin: corsOrigins }),
    express.json(),
    graphqlUploadExpress({ maxFiles: 1 }),
    expressMiddleware(apollo, {
      context: async () => ({ clients, loaders: createLoaders(clients) }),
    }),
  );

  return { app, httpServer, apollo };
}
