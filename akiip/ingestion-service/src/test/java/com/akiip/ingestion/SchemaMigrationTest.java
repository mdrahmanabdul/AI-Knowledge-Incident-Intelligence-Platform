package com.akiip.ingestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Day 3 "done when" #1: Flyway runs on startup and creates the whole schema. */
@SpringBootTest
@Import(PostgresTestContainerConfig.class)
class SchemaMigrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayAppliedV1Successfully() {
        Integer applied = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success",
                Integer.class);

        assertThat(applied).isEqualTo(1);
    }

    @Test
    void allNineTablesExist() {
        List<String> tables = jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                """, String.class);

        assertThat(tables).contains(
                "tenant", "app_user", "source_connector", "ingestion_job",
                "document", "document_version", "chunk", "chunk_embedding", "outbox");
    }

    @Test
    void pgvectorExtensionIsInstalled() {
        List<String> extensions = jdbc.queryForList(
                "SELECT extname FROM pg_extension WHERE extname = 'vector'", String.class);

        assertThat(extensions).containsExactly("vector");
    }
}
