# Unit test plan

## Approach

| Kind | Used for | Spring? | Docker? |
|---|---|---|---|
| **Plain** | Pure functions and the `rpc` wrapper: request rules, error mapping, mapping, search patterns | no | no |
| **Service** | Business rules in the services, with their dependencies mocked | no | no |
| **Repository** | The SQL: hand-written queries, constraints, cascades | yes, `@DataJdbcTest` (only the repositories and Flyway start) | yes, the docker-compose Postgres |
| **Temp folder** | `FileStorage`, the media-app equivalent of a repository | no | no |

- **Services mock their repositories** with [MockK](https://mockk.io), the usual mocking library for Kotlin. A service test checks what the service does with what the repository returns, and which repository calls it makes. It never touches a database.
- **Repositories run against real Postgres**, because the risky logic is in the SQL (`ILIKE`, joins, `DISTINCT`, cascades, constraint names). Strictly speaking these are integration tests.
- **The two halves meet at the error.** `CreditServiceTests` mocks the repository to throw a `DataIntegrityViolationException` naming `pk_credits`; `CreditRepositoryTests` proves real Postgres throws one that does.
- **One test per behaviour.** A case is only listed when it covers a rule or branch that no other case does.
- **Naming:** a test class is the class under test plus `Tests` (`MovieServiceTests`); a test method is a camelCase name for the case (`updateMovieKeepsArtwork`).

## Running the tests

`./gradlew test` from `backend/`. The repository tests connect to the docker-compose Postgres (`localhost:5433`), so start it first with `docker compose up -d postgres`.

## Step 1: Set up

- Test dependencies: MockK and Spring's Data JDBC test slice.
- Each repository test class has `@DataJdbcTest` with the datasource from `application.properties`. Each test runs in a transaction that's rolled back, and starts by emptying the tables inside it.
- Small helpers in each class to save a movie, person or credit in one line.

## Step 2: Add movie-app tests

| Test | Kind | Cases |
|---|---|---|
| `RequestRulesTests` | Plain | Calls `rpc(request)` with bad requests and checks the messages. Movie: blank title, title of 201 characters, synopsis of 5001, release date `2025-1-5`, runtime 0 and 1001, all broken at once (every one reported), missing `fields`. Person: blank name, 201-character name, 5001-character bio. Malformed id; page −1; page size 0 and 101; credit type not set; 101 ids. One valid movie and one valid person pass, which also proves the CEL rules compile. |
| `GrpcExceptionHandlerTests` | Plain | `ValidationException` → `INVALID_ARGUMENT` with its message; `NotFoundException` → `NOT_FOUND`; `AlreadyExistsException` → `ALREADY_EXISTS`; `OptimisticLockingFailureException` → `ABORTED`; anything else → `INTERNAL` with "Something went wrong" and no details; a cancellation stays a cancellation; an invalid request never runs the block |
| `MovieGrpcControllerTests` | Service mocked | The list filter and page pass through. Unsent fields reach the service as `null` and stay unset in the response; filled ones come back; an empty synopsis stays `""`. An impossible date (2025-02-30) → `INVALID_ARGUMENT` before the service runs. Attach returns the replaced key, or none |
| `PersonGrpcControllerTests` | Service mocked | The list query and page pass through. No bio → `null` in, unset out; a bio comes back |
| `CreditGrpcControllerTests` | Service mocked | Add and remove convert the credit type. Both list calls return names or titles with the credit type |
| `SearchTests` | Plain | `likePattern`: blank is no filter, the term is trimmed, `%` `_` and `\` are escaped. `offsetOf` with a page too large for an Int |
| `MovieServiceTests` | Service | List passes the patterns and offset to the repository and returns its count; create trims the title; update keeps the artwork; unknown id → `NOT_FOUND` (get, update, delete, attach); attach returns the replaced key, or null the first time; delete calls media-app after the repository; delete still succeeds when media-app throws |
| `PersonServiceTests` | Service | List passes the pattern and offset; create trims the name; update; unknown id → `NOT_FOUND`; delete calls `deleteById` |
| `CreditServiceTests` | Service | Add and remove pass the type's name; the repository throwing on `pk_credits` → `ALREADY_EXISTS` with the type in the message, on `fk_credits_movie_id` or `fk_credits_person_id` → `NOT_FOUND`, on any other constraint → rethrown; batch lookups with no ids return nothing without calling the repository |
| `MovieRepositoryTests` | Repository | `search` and `countSearch`: title ignores case; cast filter; creator filter; all three combined; a cast filter doesn't match a creator credit; two matching cast members return the movie once; an escaped `100%` matches only that text; limit, offset and a stable order. Deleting a movie removes its credits. A save based on an old read fails (`@Version`) |
| `PersonRepositoryTests` | Repository | `search` and `countSearch` by name; deleting a person removes their credits |
| `CreditRepositoryTests` | Repository | Insert and delete; the same person as cast and creator; a duplicate fails naming `pk_credits`; an unknown movie or person fails naming its foreign key; `findByMovieIds` and `findByPersonIds` return names and titles for several ids at once |

## Step 3: Add media-app tests

| Test | Kind | Cases |
|---|---|---|
| `FileStorageTests` | Temp folder | A key with `..` or an absolute path is rejected; a missing file → `NOT_FOUND`; deleting something already gone succeeds; deleting a movie folder |
| `MediaServiceTests` | Service | `FileStorage` and `MovieApi` mocked; `newFile` returns a file in a temp folder; `upload` gets a `flowOf(...)` of parts. Attach succeeds: the replaced file is deleted, or nothing is when there was none. Attach fails: the error is passed on and the new file is deleted. Deleting the replaced file fails: the upload still succeeds |
| `MediaGrpcControllerTests` | Temp folder | Real `FileStorage` and `MediaService`, `MovieApi` mocked; calls the controller with a `flowOf(...)` of messages. A whole upload is stored; malformed movie id; two movie ids; a chunk before the movie id; an empty stream; a cancelled upload leaves no file on disk; movie-app being down → `UNAVAILABLE`, and no file is left. Reading a file returns its size then its bytes; reading a missing file → `NOT_FOUND` |

## Step 4: Add frontend tests

`graphql.spec.ts` covers the fetch helper: the request it sends, joined error messages, an unreachable gateway and a response with no data.

## Left out

- **gRPC clients against a real server:** `MediaApi` and `MovieApi` are thin wrappers, and the service tests mock them.
