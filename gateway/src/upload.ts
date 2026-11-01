import * as grpc from "@grpc/grpc-js";
import { once } from "node:events";
import type { Readable } from "node:stream";
import { connected, type GrpcClient, TRANSFER_DEADLINE_MS } from "./grpc/clients.js";
import type { UploadFileResponse } from "./grpc/types.js";
import { serviceUnavailableError, toGraphQLError } from "./errors.js";

const CHUNK_BYTES = 64 * 1024;

/** Forwards the file to media-app as it arrives: the movie id, then chunks. Resolves once it's stored. */
export async function uploadFile(media: GrpcClient, movieId: string, file: Readable): Promise<UploadFileResponse> {
  // The call's deadline is long, so without this a service that's down would keep the browser waiting for all of it.
  try {
    await connected(media);
  } catch {
    file.resume();
    throw serviceUnavailableError();
  }

  const stopSending = new AbortController();
  const { promise, resolve, reject } = Promise.withResolvers<UploadFileResponse>();
  const options = { deadline: Date.now() + TRANSFER_DEADLINE_MS };
  const call: grpc.ClientWritableStream<unknown> = media.uploadFile(
    options,
    (error: grpc.ServiceError | null, response: UploadFileResponse) => {
      // media-app has answered, which can be early if it fails partway through.
      stopSending.abort();
      if (error) reject(toGraphQLError(error));
      else resolve(response);
    },
  );
  // Not awaited: the answer can arrive while this is still sending. It handles its own failures.
  void sendFile(call, movieId, file, stopSending.signal);
  return promise;
}

async function sendFile(call: grpc.ClientWritableStream<unknown>, movieId: string, file: Readable, stop: AbortSignal) {
  try {
    call.write({ movieId });
    // destroyOnReturn: false, so the rest of the file can still be read and discarded below.
    for await (const piece of file.iterator({ destroyOnReturn: false }) as AsyncIterable<Buffer>) {
      if (stop.aborted) break;
      for (let offset = 0; offset < piece.length; offset += CHUNK_BYTES) {
        // A false return means gRPC's buffer is full: wait, so the browser's progress tracks the real upload.
        if (!call.write({ chunk: piece.subarray(offset, offset + CHUNK_BYTES) })) {
          await once(call, "drain", { signal: stop });
        }
      }
    }
    // Ending the stream tells media-app the whole file was sent, so it stores it.
    if (!stop.aborted) call.end();
  } catch {
    // The browser disconnected or media-app answered.
    call.cancel();
  }
  // The browser may still be sending. Discarding the rest lets the request finish.
  file.resume();
}
