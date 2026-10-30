# Build context: repo root, because the gateway needs the .proto files exported by each backend service.

# --- Build stage: compile TypeScript ---
FROM node:24-alpine AS build
WORKDIR /app
COPY gateway/package.json gateway/package-lock.json ./
RUN npm ci
COPY gateway/tsconfig.json ./
COPY gateway/src src
RUN npm run build && npm prune --omit=dev

# --- Runtime stage ---
FROM node:24-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY gateway/package.json ./
COPY --from=build /app/node_modules node_modules
COPY --from=build /app/dist dist
# Contracts published by each service team (their -api modules).
COPY backend/movie/movie-api/src/main/proto proto/movie
COPY backend/media/media-api/src/main/proto proto/media
ENV MOVIE_PROTO_DIR=/app/proto/movie \
    MEDIA_PROTO_DIR=/app/proto/media
USER node
EXPOSE 4000
CMD ["node", "dist/index.js"]
