import * as grpc from "@grpc/grpc-js";
import type { Request, Response } from "express";
import { pipeline } from "node:stream/promises";
import { connected, type GrpcClient, TRANSFER_DEADLINE_MS } from "./grpc/clients.js";
import type { ReadFileResponse } from "./grpc/types.js";

/** GET /media/{storageKey}: streams a stored file. */
export function mediaRoute(media: GrpcClient) {
  return async (req: Request, res: Response) => {
    // Express splits the wildcard into path segments.
    const storageKey = [req.params.key].flat().join("/");
    try {
      await connected(media);
      await streamFile(media, storageKey, res);
    } catch (error) {
      sendFailure(error as grpc.ServiceError, res);
    }
  };
}

async function streamFile(media: GrpcClient, storageKey: string, res: Response) {
  const call: grpc.ClientReadableStream<ReadFileResponse> = media.readFile(
    { storageKey },
    { deadline: Date.now() + TRANSFER_DEADLINE_MS },
  );
  // Cancelling reports an error on a stream the pipeline has already finished with; without a listener it would crash.
  call.on("error", () => {});
  // The viewer left: stop reading the file.
  res.once("close", () => call.cancel());

  // The first message is read outside the pipeline, which would destroy the response on a failure such as NOT_FOUND.
  const messages = call.iterator() as AsyncIterableIterator<ReadFileResponse>;
  const { value: first } = await messages.next();
  const info = (first as ReadFileResponse).info!;
  res.set({
    "Content-Length": String(info.totalSize),
    // Safe because each key holds one fixed file forever.
    "Cache-Control": "public, max-age=31536000, immutable",
  });

  await pipeline(async function* () {
    for await (const { chunk } of messages) if (chunk) yield chunk;
  }, res);
}

function sendFailure(error: grpc.ServiceError, res: Response) {
  // Bytes have already gone out, so the only signal left is cutting the connection, which pipeline has done.
  if (res.headersSent || res.destroyed) {
    if (error.code !== grpc.status.CANCELLED && !res.writableFinished) console.warn("A file stream stopped:", error.message);
    return;
  }
  if (error.code === grpc.status.NOT_FOUND) {
    res.sendStatus(404);
  } else if (error.code === grpc.status.UNAVAILABLE || error.code === grpc.status.DEADLINE_EXCEEDED) {
    console.warn("media-app couldn't be reached:", error.message);
    res.sendStatus(503);
  } else {
    console.error("Unexpected error reading a file:", error);
    res.sendStatus(500);
  }
}
