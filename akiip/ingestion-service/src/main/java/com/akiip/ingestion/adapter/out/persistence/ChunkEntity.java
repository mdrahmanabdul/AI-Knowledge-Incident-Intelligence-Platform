package com.akiip.ingestion.adapter.out.persistence;

import com.akiip.ingestion.domain.ChunkLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/** A retrievable piece of a document version: what we search, rerank and cite. */
@Entity
@Table(name = "chunk")
public class ChunkEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "document_version_id", nullable = false)
    private UUID documentVersionId;

    @Column(name = "parent_chunk_id")
    private UUID parentChunkId;

    @Column(nullable = false)
    private int ordinal;

    @Enumerated(EnumType.STRING)
    @Column(name = "chunk_level", nullable = false)
    private ChunkLevel chunkLevel;

    @Column(name = "section_path")
    private String sectionPath;

    @Column(nullable = false)
    private String content;

    @Column(name = "context_prefix")
    private String contextPrefix;

    @Column(name = "token_count", nullable = false)
    private int tokenCount;

    @Column(name = "page_no")
    private Integer pageNo;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(nullable = false)
    @ColumnTransformer(write = "cast(? as jsonb)")
    private String metadata;

    @Column(name = "is_latest", nullable = false)
    private boolean latest;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChunkEntity() {
    }

    public ChunkEntity(UUID tenantId, UUID documentId, UUID documentVersionId, int versionNo,
                       ChunkLevel level, int ordinal, String sectionPath, String content,
                       int tokenCount, String contentHash) {
        this.id = deterministicId(documentId, versionNo, level, ordinal);
        this.tenantId = tenantId;
        this.documentId = documentId;
        this.documentVersionId = documentVersionId;
        this.chunkLevel = level;
        this.ordinal = ordinal;
        this.sectionPath = sectionPath;
        this.content = content;
        this.tokenCount = tokenCount;
        this.contentHash = contentHash;
        this.metadata = "{}";
        this.latest = true;
        this.createdAt = Instant.now();
    }

    /**
     * Same document + version + position always gives the same id, so re-processing
     * an event (Kafka may deliver twice) updates the row instead of duplicating it.
     */
    public static UUID deterministicId(UUID documentId, int versionNo, ChunkLevel level, int ordinal) {
        String key = documentId + "|" + versionNo + "|" + level + "|" + ordinal;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    public void setParentChunkId(UUID parentChunkId) { this.parentChunkId = parentChunkId; }
    public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }
    public void setMetadata(String metadataJson) { this.metadata = metadataJson; }
    public void setContextPrefix(String contextPrefix) { this.contextPrefix = contextPrefix; }
    public void markSuperseded() { this.latest = false; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getDocumentId() { return documentId; }
    public UUID getDocumentVersionId() { return documentVersionId; }
    public UUID getParentChunkId() { return parentChunkId; }
    public int getOrdinal() { return ordinal; }
    public ChunkLevel getChunkLevel() { return chunkLevel; }
    public String getSectionPath() { return sectionPath; }
    public String getContent() { return content; }
    public String getContextPrefix() { return contextPrefix; }
    public int getTokenCount() { return tokenCount; }
    public Integer getPageNo() { return pageNo; }
    public String getContentHash() { return contentHash; }
    public String getMetadata() { return metadata; }
    public boolean isLatest() { return latest; }
    public Instant getCreatedAt() { return createdAt; }
}
