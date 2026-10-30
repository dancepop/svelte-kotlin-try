# svelte-kotlin-try

A movie catalogue: Svelte (TypeScript) frontend, an Apollo GraphQL gateway, and Kotlin Spring Boot services that talk gRPC.

> Work in progress. The backend services are built. The gateway and frontend are still placeholders, so the movie features can't be reached from the browser yet.

Design docs:

- [`docs/initial-setup-plan.md`](docs/initial-setup-plan.md): project structure, tooling and Docker
- [`docs/database-design.md`](docs/database-design.md): tables, migrations and search indexes
- [`docs/backend-design.md`](docs/backend-design.md): service behaviour, gRPC contracts and the planned GraphQL schema

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
| `infra/` | Postgres init script: the `movie` role, schema and `pg_trgm` | Postgres 18 |

Each service owns its gRPC contract in an `-api` module (e.g. `backend/movie/movie-api/src/main/proto`), which other services and the gateway consume.

## Run everything (Docker)

Requires Docker.

```bash
docker compose up --build
```

Then open http://localhost:3000, which shows a placeholder page for now. The GraphQL endpoint is http://localhost:4000/graphql.

## Run locally (for development)

Requires Docker for Postgres, a JDK (17+, used to run Gradle; JDK 25 is downloaded automatically) and Node.js 24+. Run each command in its own terminal, starting from the repo root.

```bash
# 1. Postgres, with port 5432 free. The init script creates the movie role and schema.
docker run --rm -p 5432:5432 -e POSTGRES_DB=app -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -v "$PWD/infra/postgres/init:/docker-entrypoint-initdb.d:ro" postgres:18

# 2. Backend services
cd backend && ./gradlew :movie:movie-service:bootRun
cd backend && MEDIA_STORAGE_ROOT=/tmp/movie-media ./gradlew :media:media-service:bootRun

# 3. Gateway
cd gateway && npm install && npm run dev        # http://localhost:4000/graphql

# 4. Frontend
cd frontend && npm install && npm run dev       # http://localhost:5173
```

## Tests

```bash
cd backend && ./gradlew test                                 # needs Docker: movie-service tests start Postgres with Testcontainers
cd frontend && npx playwright install chromium && npm test   # browser install needed once, for component tests
```

The gateway and media-service have no tests yet. See [`docs/unit-test-plan.md`](docs/unit-test-plan.md).

## How AI tools were used

_To be completed._ Claude Code was used for planning the architecture, explaining trade-offs (GraphQL vs gRPC roles, Apollo Server vs Router, proto ownership and versioning), writing the design docs, and implementing the project setup and backend services, which were reviewed step by step.
