import * as grpc from "@grpc/grpc-js";
import { getProtoPath } from "google-proto-files";
import { GraphQLError } from "graphql";
import protobuf from "protobufjs";

const SOMETHING_WENT_WRONG = "Something went wrong, try again";

/** Turns a failed gRPC call into the error the browser sees. Unexpected details stay in the logs. */
export function toGraphQLError(error: grpc.ServiceError): GraphQLError {
  switch (error.code) {
    case grpc.status.INVALID_ARGUMENT:
      return new GraphQLError(error.details, {
        extensions: { code: "BAD_USER_INPUT", fieldErrors: fieldErrorsOf(error) },
      });
    case grpc.status.NOT_FOUND:
      return new GraphQLError(error.details, { extensions: { code: "NOT_FOUND" } });
    case grpc.status.ALREADY_EXISTS:
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

// Google's error model, from the google-proto-files package: only these two files (and their imports) are parsed.
const errorProtos = protobuf.loadSync([getProtoPath("rpc/status.proto"), getProtoPath("rpc/error_details.proto")]);
const StatusType = errorProtos.lookupType("google.rpc.Status");
const BadRequestType = errorProtos.lookupType("google.rpc.BadRequest");
const BAD_REQUEST_TYPE_URL = "type.googleapis.com/google.rpc.BadRequest";

// The decoded shapes, in camelCase as protobufjs gives them.
interface StatusMessage {
  details: { typeUrl: string; value: Uint8Array }[];
}
interface BadRequestMessage {
  fieldViolations: { field: string; description: string }[];
}

// The services send a google.rpc.Status in this trailer. Its details are `Any`s, so each says what type it holds;
// the BadRequest ones list each broken rule with its field.
function fieldErrorsOf(error: grpc.ServiceError) {
  const [details] = error.metadata.get("grpc-status-details-bin");
  if (!(details instanceof Buffer)) return [];
  const status = StatusType.decode(details) as unknown as StatusMessage;
  return status.details
    .filter((any) => any.typeUrl === BAD_REQUEST_TYPE_URL)
    .flatMap((any) => (BadRequestType.decode(any.value) as unknown as BadRequestMessage).fieldViolations)
    .map(({ field, description }) => ({ field, message: description }));
}
