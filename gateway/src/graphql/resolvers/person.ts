import { unary } from "../../grpc/clients.js";
import { nullIfNotFound } from "../../grpc/errors.js";
import type { Resolvers } from "../generated/resolvers.js";
import { toPerson, withoutNulls } from "../mappers.js";

export const personResolvers: Resolvers = {
  Query: {
    people: async (_, args, { clients }) => {
      const { search, ...paging } = withoutNulls(args);
      const { people, totalCount } = await unary(clients.person, "listPeople", { query: search, ...paging });
      return { items: people.map(toPerson), totalCount };
    },
    person: (_, { id }, { clients }) =>
      nullIfNotFound(async () => toPerson(await unary(clients.person, "getPerson", { id }))),
  },

  Mutation: {
    createPerson: async (_, { input }, { clients }) =>
      toPerson(await unary(clients.person, "createPerson", { fields: withoutNulls(input) })),
    updatePerson: async (_, { id, input }, { clients }) =>
      toPerson(await unary(clients.person, "updatePerson", { id, fields: withoutNulls(input) })),
    deletePerson: async (_, { id }, { clients }) => {
      await unary(clients.person, "deletePerson", { id });
      return true;
    },
  },

  // Credits need a join, so they're only fetched when a query asks for them.
  Person: {
    credits: (person, _, { loaders }) => loaders.creditsByPerson.load(person.id),
  },
};
