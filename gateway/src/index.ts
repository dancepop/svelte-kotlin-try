import { createApp } from "./app.js";
import { config } from "./config.js";

const { httpServer } = await createApp(config.corsOrigins);

httpServer.listen(config.port, () => {
  console.log(`Gateway ready at http://localhost:${config.port}/graphql`);
});
