# Initial Setup

This document records how the project is set up and why. The features are designed in [database-design.md](database-design.md), [backend-design.md](backend-design.md) and [frontend-design.md](frontend-design.md).

---

## 1. Architecture decisions

### Gateway: Apollo Server + Express (not Apollo Router)

Browser → GraphQL → **Apollo Server + Express** (Node/TS) → gRPC → Kotlin services.

- **gRPC is the backbone:** the Kotlin services only speak gRPC, so service-to-service communication is clearly gRPC.
- **Custom HTTP routes:** showing artwork needs a plain HTTP endpoint (`<img src>`). Express serves it next to `/graphql`; Apollo Router can't host custom routes.
- **Uploads through the gateway:** multipart GraphQL uploads let the gateway stream files to media-app over **gRPC client streaming**. Router only supports uploads as a paid enterprise feature.

### Two services, layered inside

- **movie-app** owns movies **and** cast/creators. Movies and people are linked by credits and queried together, so splitting them would turn joins into cross-service calls.
- **media-app** owns artwork files: uploads, storage and streaming. It has different scaling and storage needs from movie-app, because it handles files rather than rows.
- Inside each `-app` module, code is layered by **package**: `controller/` (gRPC entry points) → `service/` (business logic) → `repository/` (database). Layering doesn't need Gradle modules; only the published contract is its own module.

### Each service owns its `.proto` (team ownership)

- Each service has a **`-proto` module** holding its `.proto` files and generated stubs. This is the team's published contract.
- Consumers depend on the `-proto` module, never the `-app` module (e.g. `implementation(project(":movie:movie-proto"))`).
- `-proto` modules apply `maven-publish`: `./gradlew :movie:movie-proto:publishToMavenLocal` produces a jar with the `.proto` files inside, which is what a multi-repo company would push to a registry.
- The gateway reads the `.proto` files straight from the backend proto modules, by relative path. `gateway/proto` only holds a copy of `buf/validate/validate.proto`, because the contracts import it.
- **Why the contract isn't merged into the app module:** the published jar would then be the whole Spring Boot app. Also, when protobuf and Spring Boot share a module, Spring Boot 4.1's Gradle plugin auto-configures protobuf (`ProtobufPluginAction`), which clashes with our own `grpc` generator setup. Keeping `-proto` free of Spring Boot avoids both problems.

---

## 2. Repo layout

```
svelte-kotlin-try/
├── docker-compose.yml        ← starts Postgres; everything else runs on the host
├── README.md                 ← setup, how to run, how AI was used
├── docs/
├── frontend/                 ← SvelteKit + TypeScript
├── gateway/                  ← Apollo Server + Express (Node + TypeScript)
└── backend/                  ← one Gradle multi-project (≈ Maven multi-module)
    ├── settings.gradle.kts         ★ the only settings file: modules, repositories, JDK auto-download
    ├── build.gradle.kts            ★ plugin versions only (≈ parent POM <pluginManagement>)
    ├── gradle.properties           ★ build flags: parallel + build cache
    ├── gradle/libs.versions.toml   ★ all versions in one place, shared by both services
    ├── gradle/wrapper/, gradlew      Gradle wrapper (Mac/Linux; no gradlew.bat)
    ├── movie/                        plain folder, no Gradle file
    │   ├── movie-proto/ ★ build.gradle.kts · src/main/proto/movie/{Movie,Person,Credit}Service.proto
    │   └── movie-app/   ★ build.gradle.kts · Spring Boot app, gRPC on :9091
    │       └── src/main/kotlin/com/backend/movie/{controller,service,repository}/
    └── media/                        plain folder, no Gradle file
        ├── media-proto/ ★ build.gradle.kts · src/main/proto/media/MediaService.proto
        └── media-app/   ★ build.gradle.kts · Spring Boot app, gRPC on :9092
```

★ = Gradle file. A folder gets a `build.gradle.kts` only if it is listed in `include(...)`.

---

## 3. Gradle in Maven terms

| Maven | Gradle (Kotlin DSL) |
|---|---|
| `pom.xml` | `build.gradle.kts` |
| Parent POM `<modules>` | `settings.gradle.kts` → `include("movie:movie-app", ...)` |
| `<properties>` / `<dependencyManagement>` | `gradle/libs.versions.toml` (version catalog), used as `libs.xxx` |
| `<pluginManagement>` in parent | root `build.gradle.kts` → `alias(...) apply false` |
| `<repositories>` | `dependencyResolutionManagement { repositories { ... } }` in settings |
| `mvnw` + `.mvn/wrapper/` | `gradlew` + `gradle/wrapper/` (the wrapper downloads Gradle) |
| `<scope>compile</scope>` | `implementation(...)`, or `api(...)` to also expose it to consumers |
| `<scope>test</scope>` | `testImplementation(...)` |
| `<scope>provided</scope>` | `compileOnly(...)` |
| `<scope>runtime</scope>` | `runtimeOnly(...)` |
| Importing a BOM | `platform("group:bom:version")` |
| Depending on a sibling module | `implementation(project(":movie:movie-proto"))`, always built from source, never read from `~/.m2` |
| `target/` | `build/` |
| `mvn verify` (pipeline) | `./gradlew build` (compile + test + jars for every module) |
| `mvn -pl x -am test` | `./gradlew :movie:movie-app:test` (or `../../gradlew test` from inside the module) |
| `mvn spring-boot:run` | `./gradlew :movie:movie-app:bootRun` |
| `mvn install` (to `~/.m2`) | `./gradlew publishToMavenLocal` |

- Gradle build files are Kotlin code, not XML. `:movie:movie-app` is a project path: "the `movie-app` folder inside `movie`".
- **How the files work together.** Gradle runs in three phases:
  1. *Initialization* reads `settings.gradle.kts` to learn which modules exist, and loads the catalog as `libs`.
  2. *Configuration* runs every `build.gradle.kts` (root first), and the plugins add tasks.
  3. *Execution* runs the requested task plus the tasks it depends on, skipping anything up to date.

  `build` is just a task, not a fixed lifecycle phase as in Maven.
- **No inheritance.** A Maven parent POM like `cds-parent` maps to three separate things in Gradle:
  - a BOM (`platform(...)`) for versions;
  - a convention plugin for shared build config;
  - a published version catalog for library coordinates.
- **One build file per module.** A parent file can configure its children (`project(":x") { ... }`), but Gradle discourages it:
  - you lose the `plugins {}` block and the type-safe accessors;
  - the child's build is hidden in another file;
  - it blocks the configuration cache and isolated-projects optimisations.
- **`api` vs `implementation`:** the `-proto` modules declare gRPC/protobuf with `api(...)`, because those types appear in the generated code's public signatures, so consumers need them too. Everything else uses `implementation(...)`, which stays private to the module.
- Gradle writes `build/reports/problems/problems-report.html` on each build, listing warnings. It is throwaway build output.

---

## 4. Backend (`backend/`)

**Versions:** Spring Boot 4.1.2, Kotlin 2.3.21, Gradle 9.8.2, Java 25, protobuf Gradle plugin 0.10.0.

- **Packages:** `com.backend.movie` / `com.backend.media`, matching the Gradle `group`. Generated stubs live in `com.backend.<service>.api`.
- **Java 25** is the current LTS and is supported by Spring Boot 4, Kotlin 2.3 and Gradle 9.8. The gains are at runtime: faster startup with the AOT cache, compact object headers, and virtual threads without `synchronized` pinning. Each module sets it with `kotlin { jvmToolchain(25) }`.
- The **foojay toolchain resolver** (in settings) downloads JDK 25 if Gradle can't find one. Gradle doesn't detect jenv installs, so it uses its own copy in `~/.gradle/jdks`.
- **`gradle.properties`** turns on parallel builds (the two services build at the same time) and the build cache.
- **gRPC and protobuf versions come from the Spring Boot BOM.** Only the code generators (`protoc`, `protoc-gen-grpc-java`, `protoc-gen-grpc-kotlin`) are pinned, to match it.
- **No shared `subprojects {}` block.** The two `-proto` builds repeat about 20 lines of protobuf config on purpose, since each "team" owns its build.
- **`-proto` modules** generate code with:
  - `java` (built in): message classes;
  - `grpc`: Java stubs, which the Kotlin stubs build on;
  - `grpckt`: **coroutine** stubs, so service methods are `suspend fun`;
  - `kotlin` (built in): Kotlin DSL builders such as `movie { title = "Heat" }`.
- **Request rules** are `(buf.validate.field)` options in the `.proto` files, checked with protovalidate (`build.buf:protovalidate`, an `api` dependency of each `-proto` module). Its jar also holds `buf/validate/validate.proto`, which the protobuf plugin unpacks so the contracts can import it.
- **Tests:** JUnit 5 with kotlin.test assertions, MockK for mocks. The repository tests run against the docker-compose Postgres. The plan is in [unit-test-plan.md](unit-test-plan.md).
- **`-app` modules** are Spring Boot apps (`spring-boot-starter-grpc-server`). Their gRPC controllers (`controller/`) extend the generated `...CoroutineImplBase`, one per gRPC service. Spring Boot also registers the standard `grpc.health.v1.Health` service automatically.
- **Known warning:** `Configuration.setVisible(boolean) has been deprecated` comes from the protobuf plugin itself (`ProtobufPlugin.groovy`), even in 0.10.0. It is harmless until Gradle 11.

```bash
./gradlew build                                  # compile + test everything (Docker must be running)
./gradlew :movie:movie-app:bootRun           # movie-app on :9091
./gradlew :media:media-app:bootRun           # media-app on :9092
./gradlew :movie:movie-proto:publishToMavenLocal   # "export" the movie contract jar
```

---

## 5. Gateway (`gateway/`)

- **Stack:** Apollo Server 5 + `@as-integrations/express5`, Express 5, `graphql` **16** (Apollo Server 5 doesn't support 17 yet), `@grpc/grpc-js` + `@grpc/proto-loader`, TypeScript.
- `proto-loader` reads the `.proto` files at runtime from the backend proto modules.
- **Generated types:** `npm run codegen` runs before `dev`, `build` and `typecheck`. `proto-loader-gen-types` writes the gRPC clients' and messages' types to `src/grpc/generated`, and `graphql-codegen` (`codegen.ts`) writes the resolvers' types to `src/graphql/generated`. Both folders are gitignored.
- **Files:**
  - `config.ts`: env vars;
  - `index.ts`: starts the server;
  - `app.ts`: Express + Apollo, and loads `graphql/schema.graphql`;
  - `graphql/resolvers/`: one file each for movies, people and credits, merged in `index.ts`;
  - `graphql/mappers.ts`: gRPC responses → GraphQL types;
  - `graphql/loaders.ts` + `context.ts`: batches credit lookups, per request;
  - `grpc/clients.ts`: loads protos, creates clients and makes typed one-off calls (`unary`);
  - `grpc/errors.ts`: gRPC errors → GraphQL errors;
  - `media/upload.ts`: forwards an upload to media-app;
  - `media/route.ts`: `GET /media/*`.

---

## 6. Frontend (`frontend/`)

- **Stack:** SvelteKit 3, Svelte 5, Vite 8, TypeScript 6, Vitest 5, Tailwind 4, daisyUI 5. Scaffolded with `npx sv create` (minimal template, prettier, eslint, vitest, adapter-node).
- **SvelteKit 3 differs from older tutorials:**
  - config lives in `vite.config.ts`;
  - `$lib` is `#lib` (Node subpath imports), and imports include the extension: `#lib/api/graphql.ts`;
  - env vars are declared in `src/env.ts` (`defineEnvVars`) and imported from `$app/env/public`. `GRAPHQL_URL` is read at server start.
- **Tests:** Vitest unit tests in Node.
- **GraphQL calls** use a small `fetch` helper. Uploads use `XMLHttpRequest`, to get `upload.onprogress` events.

---

## 7. Docker (`docker-compose.yml`)

Docker only runs Postgres. The services, gateway and frontend run on the host.

- **Postgres 18** is the version we'd run on AWS RDS. Its credentials are local-only. It's on host port `5433`, so it doesn't clash with another Postgres on `5432`.
- **Sample data** is optional: `docker compose --profile seed up` also runs the `seed` service (see `docs/backend-design.md` §10).

---

## Open items

**Decisions for the design phase**

- [x] Artwork storage: a local folder on media-app (see `docs/backend-design.md`).
- [x] Artwork files go to media-app. movie-app keeps each movie's `artwork_key`.
- [x] Frontend GraphQL client: the small `fetch` helper (see `docs/frontend-design.md`).
