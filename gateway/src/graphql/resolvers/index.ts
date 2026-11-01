import { GraphQLUpload } from "graphql-upload-minimal";
import type { Resolvers } from "../generated/resolvers.js";
import { creditResolvers } from "./credit.js";
import { movieResolvers } from "./movie.js";
import { personResolvers } from "./person.js";

// Each resolver makes one gRPC call. The rules are in the services; the gateway only translates.
// Apollo merges these maps, so each domain keeps its Query, Mutation and field resolvers together.
export const resolvers: Resolvers[] = [{ Upload: GraphQLUpload }, movieResolvers, personResolvers, creditResolvers];
