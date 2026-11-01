import type * as grpc from "@grpc/grpc-js";
import { once } from "node:events";
import type { Readable } from "node:stream";
import { TRANSFER_DEADLINE_MS } from "../grpc/clients.js";
import { toGraphQLError } from "../grpc/errors.js";
import type { MediaServiceClient } from "../grpc/generated/media/MediaService.js";
import type { UploadFileRequest } from "../grpc/generated/media/UploadFileRequest.js";
import type { UploadFileResponse__Output } from "../grpc/generated/media/UploadFileResponse.js";

const CHUNK_BYTES = 64 * 1024;

type UploadCall = grpc.ClientWritableStream<UploadFileRequest>;

/** Forwards the file to media-app as it arrives: the movie id, then chunks. Resolves once it's stored. */
export function uploadFile(media: MediaServiceClient, movieId: string, file: Readable): Promise<UploadFileResponse__Output> {
  return new Promise((resolve, reject) => {
    const call = media.uploadFile({ deadline: Date.now() + TRANSFER_DEADLINE_MS }, (error, response) => {
      if (error) reject(toGraphQLError(error));
      else resolve(response!);
    });
    // If the browser disconnects, reading the file throws; cancelling ends the call, which rejects above.
    sendFile(call, movieId, file).catch(() => call.cancel());
  });
}

async function sendFile(call: UploadCall, movieId: string, file: Readable) {
  call.write({ movieId });
  for await (const piece of file as AsyncIterable<Buffer>) {
    for (let offset = 0; offset < piece.length; offset += CHUNK_BYTES) {
      // A false return means gRPC's buffer is full: wait, so the browser's progress tracks the real upload.
      if (!call.write({ chunk: piece.subarray(offset, offset + CHUNK_BYTES) })) await once(call, "drain");
    }
  }
  // Ending the stream tells media-app the whole file was sent, so it stores it.
  call.end();
}
