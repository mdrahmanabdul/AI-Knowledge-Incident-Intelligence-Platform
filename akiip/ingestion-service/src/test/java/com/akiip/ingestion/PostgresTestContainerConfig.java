package com.akiip.ingestion;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a real PostgreSQL 17 + pgvector container for tests (Docker Desktop must be running).
 * {@code @ServiceConnection} makes Spring Boot point the DataSource at this container
 * automatically, so the localhost:5433 settings in application.yml are ignored in tests.
 * Spring starts the container once and reuses it for every test class that imports this config.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainerConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(
                DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));
    }
}
