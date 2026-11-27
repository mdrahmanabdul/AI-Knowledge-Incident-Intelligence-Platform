package com.akiip.ingestion.adapter.out.persistence;

import com.akiip.ingestion.PostgresTestContainerConfig;
import com.akiip.ingestion.domain.ChunkLevel;
import com.akiip.ingestion.domain.Classification;
import com.akiip.ingestion.domain.DocType;
import com.akiip.ingestion.domain.JobStatus;
import com.akiip.ingestion.domain.JobType;
import com.akiip.ingestion.domain.SourceType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Day 3 "done when" #2: the repositories save and read back the full chain
 * tenant -> source -> job -> document -> version -> chunks -> embeddings -> outbox,
 * and pgvector finds the nearest chunk.
 *
 * {@code @Transactional} on a test = everything is rolled back afterwards, so tests don't affect each other.
 */
@SpringBootTest
@Import(PostgresTestContainerConfig.class)
@Transactional
class DocumentPersistenceTest {

    @Autowired TenantRepository tenants;
    @Autowired SourceConnectorRepository connectors;
    @Autowired IngestionJobRepository jobs;
    @Autowired DocumentRepository documents;
    @Autowired DocumentVersionRepository versions;
    @Autowired ChunkRepository chunks;
    @Autowired ChunkEmbeddingRepository embeddings;
    @Autowired OutboxEventRepository outbox;
    @Autowired EntityManager entityManager;

    @Test
    void savesAndReadsBackADocumentWithChunksEmbeddingsAndOutboxEvent() {
        // ---------- given: one uploaded runbook, processed into 3 chunks ----------
        TenantEntity tenant = tenants.save(new TenantEntity("Acme Payments", "acme"));
        SourceConnectorEntity uploads = connectors.save(
                new SourceConnectorEntity(tenant.getId(), SourceType.UPLOAD, "Manual uploads"));
        IngestionJobEntity job = jobs.save(
                new IngestionJobEntity(tenant.getId(), uploads.getId(), JobType.UPLOAD));

        DocumentEntity doc = documents.save(new DocumentEntity(
                tenant.getId(), uploads.getId(), "upi-timeout-runbook.pdf",
                "UPI Switch Timeout Runbook", DocType.RUNBOOK, "application/pdf", Classification.INTERNAL));

        String v1Hash = sha256("full text of version 1");
        DocumentVersionEntity v1 = versions.save(new DocumentVersionEntity(
                doc.getId(), 1, v1Hash, "raw/acme/" + doc.getId() + "/v1.pdf", 2_048L));
        doc.pointToVersion(v1.getId());

        List<String> sections = List.of("Symptoms", "Diagnosis > Step 1", "Fix");
        List<String> texts = List.of(
                "Collect requests time out after 30 seconds with ERR_TXN_TIMEOUT_504.",
                "Check active connections to the bank adapter; if they reach maxPoolSize the pool is exhausted.",
                "Increase maxPoolSize to 200 and restart the switch-api deployment.");
        List<UUID> chunkIds = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            ChunkEntity chunk = chunks.save(new ChunkEntity(
                    tenant.getId(), doc.getId(), v1.getId(), 1, ChunkLevel.CHILD, i,
                    sections.get(i), texts.get(i), 20, sha256(texts.get(i))));
            chunkIds.add(chunk.getId());
            // Fake embeddings: chunk i points along dimension i (a real model comes on Day 8)
            embeddings.save(new ChunkEmbeddingEntity(chunk.getId(), unitVector(i), "test-model"));
        }

        outbox.save(new OutboxEventEntity("DOCUMENT", doc.getId(), "document.raw.v1",
                "{\"documentId\":\"" + doc.getId() + "\",\"versionNo\":1}"));
        job.succeed(1, 1, texts.size());

        // Send all SQL to Postgres and empty the JPA cache, so the reads below really hit the database
        entityManager.flush();
        entityManager.clear();

        // ---------- then: everything reads back correctly ----------
        DocumentEntity loaded = documents
                .findByTenantIdAndSourceIdAndExternalId(tenant.getId(), uploads.getId(), "upi-timeout-runbook.pdf")
                .orElseThrow();
        assertThat(loaded.getTitle()).isEqualTo("UPI Switch Timeout Runbook");
        assertThat(loaded.getDocType()).isEqualTo(DocType.RUNBOOK);
        assertThat(loaded.getCurrentVersionId()).isEqualTo(v1.getId());

        // Duplicate-upload detection by SHA-256
        assertThat(versions.existsByDocumentIdAndContentHash(doc.getId(), v1Hash)).isTrue();
        assertThat(versions.existsByDocumentIdAndContentHash(doc.getId(), sha256("something else"))).isFalse();

        List<ChunkEntity> loadedChunks = chunks.findByDocumentVersionIdOrderByOrdinalAsc(v1.getId());
        assertThat(loadedChunks).extracting(ChunkEntity::getSectionPath).containsExactlyElementsOf(sections);
        assertThat(loadedChunks).allMatch(ChunkEntity::isLatest);

        ChunkEmbeddingEntity loadedEmbedding = embeddings.findById(chunkIds.get(2)).orElseThrow();
        assertThat(loadedEmbedding.getEmbedding()).hasSize(ChunkEmbeddingEntity.DIMENSIONS);
        assertThat(loadedEmbedding.getEmbedding()[2]).isEqualTo(1.0f);

        assertThat(jobs.findById(job.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.SUCCEEDED);

        List<OutboxEventEntity> pending = outbox.findTop100ByPublishedAtIsNullOrderByIdAsc();
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().getEventType()).isEqualTo("document.raw.v1");
        assertThat(pending.getFirst().getPayload()).contains(doc.getId().toString());
    }

    @Test
    void pgvectorReturnsTheNearestChunkFirst() {
        TenantEntity tenant = tenants.save(new TenantEntity("Acme Payments", "acme-vector"));
        SourceConnectorEntity uploads = connectors.save(
                new SourceConnectorEntity(tenant.getId(), SourceType.UPLOAD, "Manual uploads"));
        DocumentEntity doc = documents.save(new DocumentEntity(
                tenant.getId(), uploads.getId(), "guide.md", "Guide", DocType.GUIDE, "text/markdown",
                Classification.INTERNAL));
        DocumentVersionEntity v1 = versions.save(new DocumentVersionEntity(
                doc.getId(), 1, sha256("guide"), "raw/guide.md", 100L));

        List<UUID> chunkIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ChunkEntity chunk = chunks.save(new ChunkEntity(
                    tenant.getId(), doc.getId(), v1.getId(), 1, ChunkLevel.CHILD, i,
                    "Section " + i, "text " + i, 2, sha256("text " + i)));
            chunkIds.add(chunk.getId());
            embeddings.save(new ChunkEmbeddingEntity(chunk.getId(), unitVector(i), "test-model"));
        }
        entityManager.flush();

        // A "question" vector that is almost the same direction as chunk 1
        float[] question = unitVector(1);
        question[0] = 0.1f;

        List<UUID> nearest = embeddings.findNearestChunkIds(ChunkEmbeddingEntity.toVectorLiteral(question), 2);

        assertThat(nearest).first().isEqualTo(chunkIds.get(1));
    }

    @Test
    void chunkIdsAreDeterministicSoReprocessingDoesNotDuplicate() {
        UUID documentId = UUID.randomUUID();

        UUID first = ChunkEntity.deterministicId(documentId, 1, ChunkLevel.CHILD, 0);
        UUID again = ChunkEntity.deterministicId(documentId, 1, ChunkLevel.CHILD, 0);
        UUID nextVersion = ChunkEntity.deterministicId(documentId, 2, ChunkLevel.CHILD, 0);

        assertThat(again).isEqualTo(first);
        assertThat(nextVersion).isNotEqualTo(first);
    }

    // ---------- helpers ----------

    private static float[] unitVector(int dimension) {
        float[] v = new float[ChunkEmbeddingEntity.DIMENSIONS];
        v[dimension] = 1.0f;
        return v;
    }

    private static String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
