import { createApp } from "./app.js";
import { config } from "./config.js";
import { createClients } from "./grpc/clients.js";

const { httpServer } = await createApp(config.corsOrigins, createClients(config.services));

httpServer.listen(config.port, () => {
  console.log(`Gateway ready at http://localhost:${config.port}/graphql`);
});
