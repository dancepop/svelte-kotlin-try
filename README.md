# svelte-kotlin-try

A movie catalogue: Svelte (TypeScript) frontend, an Apollo GraphQL gateway, and Kotlin Spring Boot services that talk gRPC.

> Work in progress. Currently a **walking skeleton**: every piece runs and talks to the next, with no movie features yet.
> See [`docs/initial-setup-plan.md`](docs/initial-setup-plan.md) for the plan and the reasoning behind it.

## Architecture

```
Browser ──GraphQL──▶ gateway (Apollo Server + Express, :4000) ──gRPC──▶ movie-service (:9091)  movies, cast & creators
                                                               └─gRPC──▶ media-service (:9092)  artwork & video files: uploads, streaming
```

| Folder | What | Stack |
|---|---|---|
| `frontend/` | Web app | SvelteKit 3, Svelte 5, TypeScript, Vitest |
| `gateway/` | GraphQL API for the browser; calls the services over gRPC | Node, Apollo Server 5, Express 5 |
| `backend/` | Gradle multi-project with both services | Kotlin, Spring Boot 4.1, gRPC (coroutine stubs) |

Each service owns its gRPC contract in an `-api` module (e.g. `backend/movie/movie-api/src/main/proto`), which other services and the gateway consume.

## Run everything (Docker)

Requires Docker Desktop.

```bash
docker compose up --build
```

Then open http://localhost:3000. The GraphQL endpoint is http://localhost:4000/graphql.

## Run locally (for development)

Requires a JDK (17+, used to run Gradle; JDK 25 is downloaded automatically) and Node.js 24+.

```bash
# 1. Backend services (two terminals)
cd backend
./gradlew :movie:movie-service:bootRun
./gradlew :media:media-service:bootRun

# 2. Gateway
cd gateway && npm install && npm run dev        # http://localhost:4000/graphql

# 3. Frontend
cd frontend && npm install && npm run dev       # http://localhost:5173
```

## Tests

```bash
cd backend && ./gradlew test
cd gateway && npm test
cd frontend && npx playwright install chromium && npm test   # browser install needed once, for component tests
```

## How AI tools were used

_To be completed._ Claude Code was used for planning the architecture, explaining trade-offs (GraphQL vs gRPC roles, Apollo Server vs Router, proto ownership and versioning), and scaffolding the walking skeleton, which was reviewed step by step.
