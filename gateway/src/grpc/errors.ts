import * as grpc from "@grpc/grpc-js";
import { GraphQLError } from "graphql";

const SOMETHING_WENT_WRONG = "Something went wrong, try again";

/** The service is down or too slow, as opposed to the request being wrong. */
export function isServiceDown(error: grpc.ServiceError): boolean {
  return error.code === grpc.status.UNAVAILABLE || error.code === grpc.status.DEADLINE_EXCEEDED;
}

/** Turns a failed gRPC call into the error the browser sees. Unexpected details stay in the logs. */
export function toGraphQLError(error: grpc.ServiceError): GraphQLError {
  if (isServiceDown(error)) {
    console.warn("A service couldn't be reached:", error.message);
    return new GraphQLError(SOMETHING_WENT_WRONG, { extensions: { code: "SERVICE_UNAVAILABLE" } });
  }
  switch (error.code) {
    case grpc.status.INVALID_ARGUMENT:
      return new GraphQLError(error.details, { extensions: { code: "BAD_USER_INPUT" } });
    case grpc.status.NOT_FOUND:
      return new GraphQLError(error.details, { extensions: { code: "NOT_FOUND" } });
    case grpc.status.ALREADY_EXISTS:
    case grpc.status.ABORTED: // Another save changed the same row first.
      return new GraphQLError(error.details, { extensions: { code: "CONFLICT" } });
    default:
      console.error("Unexpected gRPC error:", error);
      return new GraphQLError(SOMETHING_WENT_WRONG, { extensions: { code: "INTERNAL_SERVER_ERROR" } });
  }
}

/** For single-item queries like `movie(id)`: a missing row is `null` rather than an error. */
export async function nullIfNotFound<T>(load: () => Promise<T>): Promise<T | null> {
  try {
    return await load();
  } catch (error) {
    if (error instanceof GraphQLError && error.extensions.code === "NOT_FOUND") return null;
    throw error;
  }
}
