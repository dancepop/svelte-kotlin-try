import DataLoader from "dataloader";
import { type Clients, unary } from "./grpc/clients.js";
import type { MovieCredit, PersonCredit } from "./grpc/types.js";

// movie-service accepts at most this many ids in one call, the same as its largest page.
const MAX_IDS_PER_CALL = 100;

export interface Context {
  clients: Clients;
  loaders: ReturnType<typeof createLoaders>;
}

/** Created per request, so cached results are never shared between requests. */
export function createLoaders(clients: Clients) {
  return {
    creditsByMovie: new DataLoader<string, MovieCredit[]>(
      async (movieIds) => {
        const { credits } = await unary<{ credits: MovieCredit[] }>(clients.credit, "listCreditsByMovies", { movieIds });
        return groupInOrder(movieIds, credits, (credit) => credit.movieId);
      },
      { maxBatchSize: MAX_IDS_PER_CALL },
    ),
    creditsByPerson: new DataLoader<string, PersonCredit[]>(
      async (personIds) => {
        const { credits } = await unary<{ credits: PersonCredit[] }>(clients.credit, "listCreditsByPeople", { personIds });
        return groupInOrder(personIds, credits, (credit) => credit.personId);
      },
      { maxBatchSize: MAX_IDS_PER_CALL },
    ),
  };
}

// DataLoader needs one result per id, in the order of the ids. An id with no credits gets an empty list.
function groupInOrder<T>(ids: readonly string[], items: T[], idOf: (item: T) => string): T[][] {
  const groups = Map.groupBy(items, idOf);
  return ids.map((id) => groups.get(id) ?? []);
}
