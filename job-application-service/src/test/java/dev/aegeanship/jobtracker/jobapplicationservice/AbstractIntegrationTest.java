package dev.aegeanship.jobtracker.jobapplicationservice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for integration tests. Starts a single PostgreSQL container
 * shared by all subclasses (singleton pattern; Ryuk removes it after the
 * JVM exits) and lets Spring Boot wire the datasource via
 * {@link ServiceConnection}, so Flyway migrations run against real Postgres.
 * MockMvc is available for tests that drive the full HTTP stack.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }
}
