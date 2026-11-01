import { unary } from "../../grpc/clients.js";
import type { Resolvers } from "../generated/resolvers.js";

export const creditResolvers: Resolvers = {
  Mutation: {
    addCredit: async (_, args, { clients }) => {
      await unary(clients.credit, "addCredit", args);
      return true;
    },
    removeCredit: async (_, args, { clients }) => {
      await unary(clients.credit, "removeCredit", args);
      return true;
    },
  },
};
