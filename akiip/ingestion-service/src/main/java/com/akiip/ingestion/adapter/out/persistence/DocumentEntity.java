package com.akiip.ingestion.adapter.out.persistence;

import com.akiip.ingestion.domain.Classification;
import com.akiip.ingestion.domain.DocType;
import com.akiip.ingestion.domain.DocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** The logical document. Content lives in DocumentVersionEntity; this row survives many versions. */
@Entity
@Table(name = "document")
public class DocumentEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    /** Id of the document in its source system (Confluence page id, file name for uploads...). */
    @Column(name = "external_id", nullable = false)
    private String externalId;

    private String uri;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false)
    private DocType docType;

    @Column(name = "mime_type")
    private String mimeType;

    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Classification classification;

    @Column(name = "owner_team")
    private String ownerTeam;

    @Column(name = "current_version_id")
    private UUID currentVersionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected DocumentEntity() {
    }

    public DocumentEntity(UUID tenantId, UUID sourceId, String externalId, String title,
                          DocType docType, String mimeType, Classification classification) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.sourceId = sourceId;
        this.externalId = externalId;
        this.title = title;
        this.docType = docType;
        this.mimeType = mimeType;
        this.classification = classification;
        this.status = DocumentStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void pointToVersion(UUID versionId) {
        this.currentVersionId = versionId;
        this.updatedAt = Instant.now();
    }

    public void markDeleted() {
        this.status = DocumentStatus.DELETED;
        this.deletedAt = Instant.now();
        this.updatedAt = this.deletedAt;
    }

    public void setUri(String uri) { this.uri = uri; }
    public void setOwnerTeam(String ownerTeam) { this.ownerTeam = ownerTeam; }
    public void setLanguage(String language) { this.language = language; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getSourceId() { return sourceId; }
    public String getExternalId() { return externalId; }
    public String getUri() { return uri; }
    public String getTitle() { return title; }
    public DocType getDocType() { return docType; }
    public String getMimeType() { return mimeType; }
    public String getLanguage() { return language; }
    public Classification getClassification() { return classification; }
    public String getOwnerTeam() { return ownerTeam; }
    public UUID getCurrentVersionId() { return currentVersionId; }
    public DocumentStatus getStatus() { return status; }
    public Instant getSourceUpdatedAt() { return sourceUpdatedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
