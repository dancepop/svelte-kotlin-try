# Unit test plan

## Approach

| Kind | Used for | Spring? | Docker? |
|---|---|---|---|
| **Plain** | Pure functions and the `rpc` wrapper: request rules, error mapping, mapping, search patterns | no | no |
| **Service** | Business rules in the services, with their dependencies mocked | no | no |
| **Repository** | The SQL: hand-written queries, constraints, cascades | yes, `@DataJdbcTest` (only the repositories and Flyway start) | yes, Testcontainers |
| **Temp folder** | `FileStorage`, the media-service equivalent of a repository | no | no |

- **Services mock their repositories** with [MockK](https://mockk.io), the usual mocking library for Kotlin. A service test checks what the service does with what the repository returns, and which repository calls it makes. It never touches a database.
- **Repositories run against real Postgres**, because the risky logic is in the SQL (`ILIKE`, joins, `DISTINCT`, cascades, constraint names). Strictly speaking these are integration tests, and the README says so.
- **The two halves meet at the error.** `CreditServiceTests` mocks the repository to throw a `DataIntegrityViolationException` naming `pk_credits`; `CreditRepositoryTests` proves real Postgres throws one that does.
- **One test per behaviour.** A case is only listed when it covers a rule or branch that no other case does.
- **Naming:** a test class is the class under test plus `Tests` (`MovieServiceTests`); a test method is a camelCase name for the case (`updateMovieKeepsArtwork`).

## Running the tests

`./gradlew test` from `backend/`. Only the repository tests need Docker running.

Colima needs two variables set in the shell:

```bash
export DOCKER_HOST=unix://$HOME/.colima/apple-profile/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

The first run pulls Testcontainers' cleanup image, so WARP must be on DNS-only.

Colima forwards a container's port to the Mac a moment after the container starts, so `RepositoryTestBase` waits until the port answers before connecting.

## Step 1: Set up

- Test dependencies: MockK, Spring's Data JDBC test slice and Testcontainers support.
- A `RepositoryTestBase` class: `@DataJdbcTest` on a `postgres:18` container started with the same init script as compose, so Flyway creates the real tables. Each test runs in a transaction that's rolled back.
- Small helpers to save a movie, person or credit in one line.

## Step 2: Add movie-service tests

| Test | Kind | Cases |
|---|---|---|
| `RequestRulesTests` | Plain | Calls `rpc(request)` with bad requests and checks the field violations. Movie: blank title, title of 201 characters, synopsis of 5001, release date `2025-1-5`, runtime 0 and 1001, all broken at once (every one reported), missing `fields`. Person: blank name, 201-character name, 5001-character bio. Malformed id; page −1; page size 0 and 101; credit type not set; 101 ids. One valid movie and one valid person pass, which also proves the CEL rules compile. |
| `GrpcExceptionHandlerTests` | Plain | `ValidationException` → `INVALID_ARGUMENT` with a `BadRequest` holding each field and message; `NotFoundException` → `NOT_FOUND`; `AlreadyExistsException` → `ALREADY_EXISTS`; anything else → `INTERNAL` with "Something went wrong" and no details; a cancellation stays a cancellation; an invalid request never runs the block |
| `ProtoMappersTests` | Plain | `parseDate`: a real date, and 2025-02-30 rejected on `releaseDate`. Credit type both ways. A movie with no synopsis, date, runtime or artwork leaves those fields unset |
| `SearchTests` | Plain | `likePattern`: blank is no filter, the term is trimmed, `%` `_` and `\` are escaped. `offsetOf` with a page too large for an Int |
| `MovieServiceTests` | Service | List passes the patterns and offset to the repository and returns its count; create trims the title; update keeps the artwork and moves `updatedAt`; unknown id → `NOT_FOUND` (get, update, delete, attach); attach returns the replaced key, or null the first time; delete calls media-service after the repository; delete still succeeds when media-service throws |
| `PersonServiceTests` | Service | List passes the pattern and offset; create trims the name; update; unknown id → `NOT_FOUND`; delete calls `deleteById` |
| `CreditServiceTests` | Service | Add and remove pass the type's name; the repository throwing on `pk_credits` → `ALREADY_EXISTS` with the type in the message, on `fk_credits_movie_id` or `fk_credits_person_id` → `NOT_FOUND`, on any other constraint → rethrown; batch lookups with no ids return nothing without calling the repository |
| `MovieRepositoryTests` | Repository | `search` and `countSearch`: title ignores case; cast filter; creator filter; all three combined; a cast filter doesn't match a creator credit; two matching cast members return the movie once; an escaped `100%` matches only that text; limit, offset and a stable order. Deleting a movie removes its credits |
| `PersonRepositoryTests` | Repository | `search` and `countSearch` by name; deleting a person removes their credits |
| `CreditRepositoryTests` | Repository | Insert and delete; the same person as cast and creator; a duplicate fails naming `pk_credits`; an unknown movie or person fails naming its foreign key; `findByMovieIds` and `findByPersonIds` return names and titles for several ids at once |

## Step 3: Add media-service tests

| Test | Kind | Cases |
|---|---|---|
| `FileStorageTests` | Temp folder | A key with `..` or an absolute path is rejected; a missing file → `NOT_FOUND`; deleting something already gone succeeds; deleting a movie folder |
| `UploadServiceTests` | Service | `FileStorage` and `MovieClient` mocked; `newFile` returns a file in a temp folder. Attach succeeds: the replaced file is deleted, or nothing is when there was none. Attach fails: the error is passed on and `cleanUp` deletes the new file. Deleting the replaced file fails: the upload still succeeds |
| `MediaGrpcControllerTests` | Temp folder | Real `FileStorage` and `UploadService`, `MovieClient` mocked; calls the controller with a `flowOf(...)` of messages. A whole upload is stored; malformed movie id; no movie id; two movie ids; a chunk before the movie id; no `complete`, and nothing is left on disk. Reading a file returns its size then its bytes; reading a missing file → `NOT_FOUND` |

## Step 4: Add gateway and frontend tests alongside the code

- **Gateway:** gRPC to GraphQL error mapping; artwork URL building; a movie with no artwork; timestamp conversion. Credit loaders: one RPC for a whole page, results returned in a different order, a movie with no credits.
- **Frontend:** the search parser (`heat cast:david creator:"michael mann"`, quotes, a repeated filter, unknown prefixes, empty input), the artwork file check (allowed and other extensions, exactly 5 MB and one byte over), plus loading and error states. `graphql.spec.ts` stays, since the fetch helper was kept.

## Left out

- **Concurrent edits:** the last save wins, so there's no behaviour to test.
- **Controller tests:** the controllers only convert types and call a service. `RequestRulesTests` covers what they reject, and the mappers have their own test.
- **gRPC clients against a real server:** `MediaClient` and `MovieClient` are thin wrappers, and the service tests mock them.
