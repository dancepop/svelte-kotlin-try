# Initial Setup

This document records how the project is set up and why. Movie, cast and artwork features come in the next phase (class and API design).

---

## 1. Architecture decisions

### Gateway: Apollo Server + Express (not Apollo Router)

Browser → GraphQL → **Apollo Server + Express** (Node/TS) → gRPC → Kotlin services.

- **gRPC is the backbone:** the Kotlin services only speak gRPC, so service-to-service communication is clearly gRPC.
- **Custom HTTP routes:** showing artwork needs a plain HTTP endpoint (`<img src>`). Express serves it next to `/graphql`; Apollo Router can't host custom routes.
- **Uploads through the gateway:** multipart GraphQL uploads let the gateway stream files to media-service over **gRPC client streaming**. Router only supports uploads as a paid enterprise feature.

### Two services, layered inside

- **movie-service** owns movies **and** cast/creators. People are attributes of a movie and are queried and edited together, so splitting them would turn joins into cross-service calls.
- **media-service** owns artwork files: uploads, storage and streaming. It has different scaling and storage needs from movie-service, because it handles files rather than rows.
- Inside each `-service` module, code is layered by **package**: `controller/` (gRPC entry points) → `service/` (business logic) → `repository/` (database). Layering doesn't need Gradle modules; only the published contract is its own module.

### Each service owns its `.proto` (team ownership)

- Each service has an **`-api` module** holding its `.proto` files and generated stubs. This is the team's published contract.
- Consumers depend on the `-api` module, never the `-service` module (e.g. `implementation(project(":movie:movie-api"))`).
- `-api` modules apply `maven-publish`: `./gradlew :movie:movie-api:publishToMavenLocal` produces a jar with the `.proto` files inside, which is what a multi-repo company would push to a registry.
- The gateway keeps its own copy of the `.proto` files in `gateway/proto`, which must be re-copied when a contract changes. With separate teams, it would install them as a versioned package instead.
- **Why the contract isn't merged into the app module:** the published jar would then be the whole Spring Boot app. Also, when protobuf and Spring Boot share a module, Spring Boot 4.1's Gradle plugin auto-configures protobuf (`ProtobufPluginAction`), which clashes with our own `grpc` generator setup. Keeping `-api` free of Spring Boot avoids both problems.

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
    │   ├── movie-api/       ★ build.gradle.kts · src/main/proto/movie/movie_service.proto
    │   └── movie-service/   ★ build.gradle.kts · Spring Boot app, gRPC on :9091
    │       └── src/main/kotlin/com/backend/movie/{controller,service,repository}/
    └── media/                        plain folder, no Gradle file
        ├── media-api/       ★ build.gradle.kts · src/main/proto/media/media_service.proto
        └── media-service/   ★ build.gradle.kts · Spring Boot app, gRPC on :9092
```

★ = Gradle file. A folder gets a `build.gradle.kts` only if it is listed in `include(...)`.

---

## 3. Gradle in Maven terms

| Maven | Gradle (Kotlin DSL) |
|---|---|
| `pom.xml` | `build.gradle.kts` |
| Parent POM `<modules>` | `settings.gradle.kts` → `include("movie:movie-service", ...)` |
| `<properties>` / `<dependencyManagement>` | `gradle/libs.versions.toml` (version catalog), used as `libs.xxx` |
| `<pluginManagement>` in parent | root `build.gradle.kts` → `alias(...) apply false` |
| `<repositories>` | `dependencyResolutionManagement { repositories { ... } }` in settings |
| `mvnw` + `.mvn/wrapper/` | `gradlew` + `gradle/wrapper/` (the wrapper downloads Gradle) |
| `<scope>compile</scope>` | `implementation(...)`, or `api(...)` to also expose it to consumers |
| `<scope>test</scope>` | `testImplementation(...)` |
| `<scope>provided</scope>` | `compileOnly(...)` |
| `<scope>runtime</scope>` | `runtimeOnly(...)` |
| Importing a BOM | `platform("group:bom:version")` |
| Depending on a sibling module | `implementation(project(":movie:movie-api"))`, always built from source, never read from `~/.m2` |
| `target/` | `build/` |
| `mvn verify` (pipeline) | `./gradlew build` (compile + test + jars for every module) |
| `mvn -pl x -am test` | `./gradlew :movie:movie-service:test` (or `../../gradlew test` from inside the module) |
| `mvn spring-boot:run` | `./gradlew :movie:movie-service:bootRun` |
| `mvn install` (to `~/.m2`) | `./gradlew publishToMavenLocal` |

- Gradle build files are Kotlin code, not XML. `:movie:movie-service` is a project path: "the `movie-service` folder inside `movie`".
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
- **`api` vs `implementation`:** the `-api` modules declare gRPC/protobuf with `api(...)`, because those types appear in the generated code's public signatures, so consumers need them too. Everything else uses `implementation(...)`, which stays private to the module.
- Gradle writes `build/reports/problems/problems-report.html` on each build, listing warnings. It is throwaway build output.

---

## 4. Backend (`backend/`)

**Versions:** Spring Boot 4.1.2, Kotlin 2.3.21, Gradle 9.8.2, Java 25, protobuf Gradle plugin 0.10.0.

- **Packages:** `com.backend.movie` / `com.backend.media`, matching the Gradle `group`. Generated stubs live in `com.backend.<service>.api`.
- **Java 25** is the current LTS and is supported by Spring Boot 4, Kotlin 2.3 and Gradle 9.8. The gains are at runtime: faster startup with the AOT cache, compact object headers, and virtual threads without `synchronized` pinning. Each module sets it with `kotlin { jvmToolchain(25) }`.
- The **foojay toolchain resolver** (in settings) downloads JDK 25 if Gradle can't find one. Gradle doesn't detect jenv installs, so it uses its own copy in `~/.gradle/jdks`.
- **`gradle.properties`** turns on parallel builds (the two services build at the same time) and the build cache.
- **gRPC and protobuf versions come from the Spring Boot BOM.** Only the code generators (`protoc`, `protoc-gen-grpc-java`, `protoc-gen-grpc-kotlin`) are pinned, to match it.
- **No shared `subprojects {}` block.** The two `-api` builds repeat about 20 lines of protobuf config on purpose, since each "team" owns its build.
- **`-api` modules** generate code with:
  - `java` (built in): message classes;
  - `grpc`: Java stubs, which the Kotlin stubs build on;
  - `grpckt`: **coroutine** stubs, so service methods are `suspend fun`;
  - `kotlin` (built in): Kotlin DSL builders such as `movie { title = "Heat" }`.
- **`-service` modules** are Spring Boot apps (`spring-boot-starter-grpc-server`). Each has one gRPC controller (`controller/`) extending the generated `...CoroutineImplBase`. Spring Boot also registers the standard `grpc.health.v1.Health` service automatically.
- **Known warning:** `Configuration.setVisible(boolean) has been deprecated` comes from the protobuf plugin itself (`ProtobufPlugin.groovy`), even in 0.10.0. It is harmless until Gradle 11.

```bash
./gradlew build                                  # compile + test everything
./gradlew :movie:movie-service:bootRun           # movie-service on :9091
./gradlew :media:media-service:bootRun           # media-service on :9092
./gradlew :movie:movie-api:publishToMavenLocal   # "export" the movie contract jar
```

---

## 5. Gateway (`gateway/`)

- **Stack:** Apollo Server 5 + `@as-integrations/express5`, Express 5, `graphql` **16** (Apollo Server 5 doesn't support 17 yet), `@grpc/grpc-js` + `@grpc/proto-loader`, TypeScript, Vitest.
- `proto-loader` reads the `.proto` files at runtime from `gateway/proto`, so there's no gRPC code-generation step.
- **Files:**
  - `config.ts`: env vars;
  - `grpc/clients.ts`: loads protos and creates clients;
  - `schema.ts` + `resolvers.ts`: the GraphQL API;
  - `app.ts`: Express + Apollo + `/healthz`;
  - `index.ts`: starts the server.

---

## 6. Frontend (`frontend/`)

- **Stack:** SvelteKit 3, Svelte 5, Vite 8, TypeScript 6, Vitest 5. Scaffolded with `npx sv create` (minimal template, prettier, eslint, vitest, adapter-node).
- **SvelteKit 3 differs from older tutorials:**
  - config lives in `vite.config.ts`;
  - `$lib` is `#lib` (Node subpath imports), and imports include the extension: `#lib/api/graphql.ts`;
  - env vars are declared in `src/env.ts` (`defineEnvVars`) and imported from `$app/env/public`. `GRAPHQL_URL` is read at server start.
- **Tests:** Vitest unit tests in Node, plus component tests in headless Chromium via Playwright (run `npx playwright install chromium` once).
- **GraphQL calls** use a small `fetch` helper for now. Uploads will use `XMLHttpRequest`, to get `upload.onprogress` events.

---

## 7. Docker (`docker-compose.yml`)

Docker only runs Postgres. The services, gateway and frontend run on the host.

- **Postgres 18** is the version we'd run on AWS RDS. Its credentials are local-only. It's on host port `5433`, so it doesn't clash with another Postgres on `5432`.

---

## Open items

**Decisions for the design phase**

- [x] Artwork storage: a local folder on media-service (see `docs/backend-design.md`).
- [x] Artwork files go to media-service. movie-service keeps each movie's `artwork_key`.
- [ ] Frontend GraphQL client: Apollo Client core or urql?
