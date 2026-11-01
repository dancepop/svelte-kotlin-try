import * as grpc from "@grpc/grpc-js";
import { GraphQLError } from "graphql";

const SOMETHING_WENT_WRONG = "Something went wrong, try again";

/** Turns a failed gRPC call into the error the browser sees. Unexpected details stay in the logs. */
export function toGraphQLError(error: grpc.ServiceError): GraphQLError {
  switch (error.code) {
    case grpc.status.INVALID_ARGUMENT:
      return new GraphQLError(error.details, { extensions: { code: "BAD_USER_INPUT" } });
    case grpc.status.NOT_FOUND:
      return new GraphQLError(error.details, { extensions: { code: "NOT_FOUND" } });
    case grpc.status.ALREADY_EXISTS:
    case grpc.status.ABORTED: // Another save changed the same row first.
      return new GraphQLError(error.details, { extensions: { code: "CONFLICT" } });
    case grpc.status.UNAVAILABLE:
    case grpc.status.DEADLINE_EXCEEDED:
      console.warn("A service couldn't be reached:", error.message);
      return serviceUnavailableError();
    case grpc.status.CANCELLED:
      // The browser left, so nobody will see this error and nothing went wrong.
      return new GraphQLError("Cancelled", { extensions: { code: "CANCELLED" } });
    default:
      console.error("Unexpected gRPC error:", error);
      return new GraphQLError(SOMETHING_WENT_WRONG, { extensions: { code: "INTERNAL_SERVER_ERROR" } });
  }
}

export function serviceUnavailableError(): GraphQLError {
  return new GraphQLError(SOMETHING_WENT_WRONG, { extensions: { code: "SERVICE_UNAVAILABLE" } });
}

