# Unit test plan

## Approach

| Kind | Used for | Tools |
|---|---|---|
| Plain unit tests | Pure logic such as validation, error mapping, file detection and parsing | JUnit or vitest only |
| Service tests on real Postgres | Rules enforced in SQL, such as constraints, cascades and search | Testcontainers, `FakeMediaClient` |
| Tests on a temp folder | Everything in media-service | JUnit `@TempDir`, `FakeMovieClient` |

Only the service tests on real Postgres need Docker.

- **Repositories stay real.** The risky logic is in the SQL, so these tests run it against Postgres. Strictly speaking they are integration tests, and the README will say so.
- **Fakes are hand-written.** There are only two small interfaces to fake, `MediaClient` and `MovieClient`, so no mocking library is needed.

## Running the tests

`./gradlew test` from `backend/`, with Docker running.

Colima needs two variables set in the shell:

```bash
export DOCKER_HOST=unix://$HOME/.colima/apple-profile/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

The first run pulls Testcontainers' cleanup image, so WARP must be on DNS-only.

## Step 1: Set up the movie-service tests

Add an `IntegrationTest` base class (Testcontainers on `postgres:18`, with the same init script as compose), a `FakeMediaClient` and a way to seed people. Set the Colima variables and switch WARP to DNS-only.

## Step 2: Add movie-service tests

| Test | Cases |
|---|---|
| `ValidationTest` (plain) | Title and synopsis length limits; runtime bounds; real and malformed dates; every broken rule reported at once; paging bounds; the id count limit |
| `SearchTest` (plain) | Escaping in search patterns; page offsets too large for an Int |
| `MovieServiceTest` | Create, update and delete; unknown ids; editing keeps the artwork; attach returns the replaced key or null; delete still succeeds when media-service is down; delete cascades to credits |
| `MovieSearchTest` | Title, cast and creator filters combined; two Davids in one movie returns it once; a cast filter doesn't match a creator credit; wildcards are plain text; `totalCount`; stable order across pages |
| `PersonServiceTest` | Search; unknown ids |
| `CreditServiceTest` | Add and remove; same person as cast and creator; duplicate credit; unknown movie or person; batch lookups with no ids |
| `GrpcExceptionHandlerTest` (plain) | Each exception maps to its status; one `field-error-bin` trailer per broken rule; unknown errors become `INTERNAL` without leaking details; cancellation stays a cancellation |
| `MovieGrpcControllerTest`, `PersonGrpcControllerTest`, `CreditGrpcControllerTest` | A broken rule is rejected and the service isn't called; valid requests reach the service |
| `ProtoMappersTest` (plain) | Credit type mapping both ways; `UNSPECIFIED` is rejected; unset optional fields stay unset; malformed uuid |

## Step 3: Add media-service tests

| Test | Cases |
|---|---|
| `FileStorageTest` | Traversal with `..`; absolute paths; missing file; deleting something already gone; deleting a movie folder |
| `UploadServiceTest` | See below |
| `MediaGrpcControllerTest` | Upload streams: no movie id, two movie ids, chunk before movie id, stream ends without `complete` and nothing is stored. Reads: whole file, missing file |

`UploadServiceTest` covers:

- **Attach succeeds:** the replaced file is deleted, or nothing is deleted if there was no earlier file
- **Attach fails:** the new file is deleted and the error is passed on
- **Delete fails:** the upload still succeeds

## Step 4: Add gateway and frontend tests alongside the code

- **Gateway:** gRPC to GraphQL error mapping; artwork URL building; a movie with no artwork; timestamp conversion. Credit loaders: one RPC for a whole page, results returned in a different order, a movie with no credits.
- **Frontend:** the search parser (`heat cast:david creator:"michael mann"`, quotes, a repeated filter, unknown prefixes, empty input), the artwork file check (allowed and other extensions, exactly 5 MB and one byte over), plus loading and error states once the pages are designed. `graphql.spec.ts` stays or goes depending on the Apollo or urql choice.

## Left out

- **Concurrent edits:** the last save wins, so there's no behaviour to test.
- **gRPC clients against a real server:** `MediaClient` and `MovieClient` are thin wrappers, and the fakes stand in for them.
- **Separate repository test classes:** the service tests already run these queries.
