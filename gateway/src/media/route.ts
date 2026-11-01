import * as grpc from "@grpc/grpc-js";
import type { Request, Response } from "express";
import { pipeline } from "node:stream/promises";
import { TRANSFER_DEADLINE_MS } from "../grpc/clients.js";
import { isServiceDown } from "../grpc/errors.js";
import type { MediaServiceClient } from "../grpc/generated/media/MediaService.js";
import type { ReadFileResponse__Output } from "../grpc/generated/media/ReadFileResponse.js";

/** GET /media/{storageKey}: streams a stored file. */
export function mediaRoute(media: MediaServiceClient) {
  return async (req: Request, res: Response) => {
    // Express splits the wildcard into path segments.
    const storageKey = [req.params.key].flat().join("/");
    try {
      await streamFile(media, storageKey, res);
    } catch (error) {
      sendFailure(error as grpc.ServiceError, res);
    }
  };
}

async function streamFile(media: MediaServiceClient, storageKey: string, res: Response) {
  const call = media.readFile({ storageKey }, { deadline: Date.now() + TRANSFER_DEADLINE_MS });
  // Cancelling reports an error on a stream the pipeline has already finished with; without a listener it would crash.
  call.on("error", () => {});
  // The viewer left: stop reading the file.
  res.once("close", () => call.cancel());

  // The first message is read outside the pipeline, which would destroy the response on a failure such as NOT_FOUND.
  const messages = call.iterator() as AsyncIterableIterator<ReadFileResponse__Output>;
  const { value: first } = await messages.next();
  const info = (first as ReadFileResponse__Output).info!;
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
  // Bytes have already gone out, or the viewer left: pipeline has already cut the connection.
  if (res.headersSent || res.destroyed) return;
  if (error.code === grpc.status.NOT_FOUND) {
    res.sendStatus(404);
  } else if (isServiceDown(error)) {
    console.warn("media-app couldn't be reached:", error.message);
    res.sendStatus(503);
  } else {
    console.error("Unexpected error reading a file:", error);
    res.sendStatus(500);
  }
}
