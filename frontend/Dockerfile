# --- Build stage: SvelteKit + adapter-node produces a standalone Node server in build/ ---
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json .npmrc ./
RUN npm ci
COPY . .
RUN npm run build && npm prune --omit=dev

# --- Runtime stage ---
FROM node:24-alpine
WORKDIR /app
ENV NODE_ENV=production \
    PORT=3000
COPY package.json ./
COPY --from=build /app/node_modules node_modules
COPY --from=build /app/build build
USER node
EXPOSE 3000
CMD ["node", "build"]
