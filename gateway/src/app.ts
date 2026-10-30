import { ApolloServer } from "@apollo/server";
import { ApolloServerPluginDrainHttpServer } from "@apollo/server/plugin/drainHttpServer";
import { expressMiddleware } from "@as-integrations/express5";
import cors from "cors";
import express from "express";
import http from "node:http";
import { resolvers } from "./resolvers.js";
import { typeDefs } from "./schema.js";

/** Express hosts Apollo on /graphql; plain HTTP routes (health, later artwork and video streaming) sit next to it. */
export async function createApp(corsOrigins: string[]) {
  const app = express();
  const httpServer = http.createServer(app);

  const apollo = new ApolloServer({
    typeDefs,
    resolvers,
    plugins: [ApolloServerPluginDrainHttpServer({ httpServer })],
  });
  await apollo.start();

  app.get("/healthz", (_req, res) => {
    res.json({ status: "ok" });
  });
  app.use("/graphql", cors({ origin: corsOrigins }), express.json(), expressMiddleware(apollo));

  return { app, httpServer, apollo };
}
