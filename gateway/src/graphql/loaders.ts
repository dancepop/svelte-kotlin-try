import DataLoader from "dataloader";
import { type Clients, unary } from "../grpc/clients.js";
import type { MovieCredit__Output } from "../grpc/generated/movie/MovieCredit.js";
import type { PersonCredit__Output } from "../grpc/generated/movie/PersonCredit.js";

export function createLoaders(clients: Clients) {
  return {
    creditsByMovie: new DataLoader<string, MovieCredit__Output[]>(async (movieIds) => {
      const { credits } = await unary(clients.credit, "listCreditsByMovies", { movieIds: [...movieIds] });
      return groupInOrder(movieIds, credits, (credit) => credit.movieId);
    }),
    creditsByPerson: new DataLoader<string, PersonCredit__Output[]>(async (personIds) => {
      const { credits } = await unary(clients.credit, "listCreditsByPeople", { personIds: [...personIds] });
      return groupInOrder(personIds, credits, (credit) => credit.personId);
    }),
  };
}

// DataLoader needs one result per id, in the order of the ids. An id with no credits gets an empty list.
function groupInOrder<T>(ids: readonly string[], items: T[], idOf: (item: T) => string): T[][] {
  const groups = Map.groupBy(items, idOf);
  return ids.map((id) => groups.get(id) ?? []);
}
