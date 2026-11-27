package com.akiip.ingestion.adapter.out.persistence;

import com.akiip.ingestion.domain.JobStatus;
import com.akiip.ingestion.domain.JobType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.util.UUID;

/** A work ticket for one ingestion run. The UI polls it to show progress. */
@Entity
@Table(name = "ingestion_job")
public class IngestionJobEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(name = "docs_seen", nullable = false)
    private int docsSeen;

    @Column(name = "docs_changed", nullable = false)
    private int docsChanged;

    @Column(name = "chunks_indexed", nullable = false)
    private int chunksIndexed;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "error_sample")
    @ColumnTransformer(write = "cast(? as jsonb)")
    private String errorSample;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected IngestionJobEntity() {
    }

    public IngestionJobEntity(UUID tenantId, UUID sourceId, JobType type) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.sourceId = sourceId;
        this.type = type;
        this.status = JobStatus.QUEUED;
        this.createdAt = Instant.now();
    }

    public void start() {
        this.status = JobStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void succeed(int docsSeen, int docsChanged, int chunksIndexed) {
        this.docsSeen = docsSeen;
        this.docsChanged = docsChanged;
        this.chunksIndexed = chunksIndexed;
        this.status = JobStatus.SUCCEEDED;
        this.finishedAt = Instant.now();
    }

    public void fail(String errorJson) {
        this.errorCount++;
        this.errorSample = errorJson;
        this.status = JobStatus.FAILED;
        this.finishedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getSourceId() { return sourceId; }
    public JobType getType() { return type; }
    public JobStatus getStatus() { return status; }
    public int getDocsSeen() { return docsSeen; }
    public int getDocsChanged() { return docsChanged; }
    public int getChunksIndexed() { return chunksIndexed; }
    public int getErrorCount() { return errorCount; }
    public String getErrorSample() { return errorSample; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
