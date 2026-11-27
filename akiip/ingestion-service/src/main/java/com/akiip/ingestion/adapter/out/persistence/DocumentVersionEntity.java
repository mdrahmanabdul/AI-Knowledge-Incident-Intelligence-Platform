package com.akiip.ingestion.adapter.out.persistence;

import com.akiip.ingestion.domain.VersionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One version of a document's content. content_hash (SHA-256) lets us skip duplicate uploads. */
@Entity
@Table(name = "document_version")
public class DocumentVersionEntity {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "raw_object_key", nullable = false)
    private String rawObjectKey;

    @Column(name = "parsed_object_key")
    private String parsedObjectKey;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    private String author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VersionStatus status;

    @Column(name = "embedding_model")
    private String embeddingModel;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @Column(name = "ingested_at", nullable = false, updatable = false)
    private Instant ingestedAt;

    protected DocumentVersionEntity() {
    }

    public DocumentVersionEntity(UUID documentId, int versionNo, String contentHash,
                                 String rawObjectKey, Long sizeBytes) {
        this.id = UUID.randomUUID();
        this.documentId = documentId;
        this.versionNo = versionNo;
        this.contentHash = contentHash;
        this.rawObjectKey = rawObjectKey;
        this.sizeBytes = sizeBytes;
        this.status = VersionStatus.RECEIVED;
        this.ingestedAt = Instant.now();
    }

    public void markParsed(String parsedObjectKey) {
        this.parsedObjectKey = parsedObjectKey;
        this.status = VersionStatus.PARSED;
    }

    public void markIndexed(String embeddingModel, int chunkCount) {
        this.embeddingModel = embeddingModel;
        this.chunkCount = chunkCount;
        this.status = VersionStatus.INDEXED;
    }

    public void markFailed() { this.status = VersionStatus.FAILED; }

    public UUID getId() { return id; }
    public UUID getDocumentId() { return documentId; }
    public int getVersionNo() { return versionNo; }
    public String getContentHash() { return contentHash; }
    public String getRawObjectKey() { return rawObjectKey; }
    public String getParsedObjectKey() { return parsedObjectKey; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getAuthor() { return author; }
    public VersionStatus getStatus() { return status; }
    public String getEmbeddingModel() { return embeddingModel; }
    public Integer getChunkCount() { return chunkCount; }
    public Instant getIngestedAt() { return ingestedAt; }
}
