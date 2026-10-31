package com.backend.movie.repository

import com.backend.movie.entities.Movie
import com.backend.movie.entities.Person
import com.backend.movie.enums.CreditType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.wait.strategy.WaitAllStrategy
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.MountableFile
import java.util.UUID

/**
 * Base for the repository tests: a real Postgres 18 in Docker, set up like docker-compose's (the same init script,
 * then Flyway creates the tables). @DataJdbcTest starts only the repositories and Flyway, and rolls each test back
 * when it ends, so every test starts from empty tables.
 */
@DataJdbcTest
// Keep the Postgres below; by default the slice would look for an in-memory database instead.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class RepositoryTestBase {

    @Autowired
    protected lateinit var movies: MovieRepository

    @Autowired
    protected lateinit var people: PersonRepository

    @Autowired
    protected lateinit var credits: CreditRepository

    // Each returns the new row's id, which Postgres fills in on INSERT.
    protected fun movie(title: String): UUID = movies.save(Movie(title = title)).id!!

    protected fun person(name: String): UUID = people.save(Person(name = name)).id!!

    protected fun credit(movieId: UUID, personId: UUID, type: CreditType) {
        credits.insert(movieId, personId, type.name)
    }

    companion object {
        // One container for every repository test class. Testcontainers removes it when the test run ends.
        private val postgres = PostgreSQLContainer("postgres:18")
            .withDatabaseName("app")
            .withUsername("postgres")
            .withPassword("postgres")
            .withCopyFileToContainer(
                // Tests run from the module folder, backend/movie/movie-service.
                MountableFile.forHostPath("../../../infra/postgres/init/01-role-and-schema.sql"),
                "/docker-entrypoint-initdb.d/01-role-and-schema.sql",
            )
            // Postgres's default wait only reads its log. Colima forwards the port to the Mac a moment later,
            // so also wait until the port answers from here.
            .waitingFor(
                WaitAllStrategy()
                    .withStrategy(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2))
                    .withStrategy(Wait.forListeningPort()),
            )
            .apply { start() }

        // The tests connect as the movie role from application.properties; only the address changes.
        @JvmStatic
        @DynamicPropertySource
        fun datasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
        }
    }
}
