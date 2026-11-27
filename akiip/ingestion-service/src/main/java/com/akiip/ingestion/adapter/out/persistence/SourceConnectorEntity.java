package com.akiip.ingestion.adapter.out.persistence;

import com.akiip.ingestion.domain.ConnectorStatus;
import com.akiip.ingestion.domain.SourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "source_connector")
public class SourceConnectorEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType type;

    @Column(nullable = false)
    private String name;

    /** JSON text. Postgres stores it as JSONB; the cast lets us bind a plain String. */
    @Column(nullable = false)
    @ColumnTransformer(write = "cast(? as jsonb)")
    private String config;

    /** Pointer to a secret store entry (e.g. Vault path). Never the secret itself. */
    @Column(name = "secret_ref")
    private String secretRef;

    @Column(name = "sync_schedule")
    private String syncSchedule;

    @Column(name = "sync_cursor")
    @ColumnTransformer(write = "cast(? as jsonb)")
    private String syncCursor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectorStatus status;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SourceConnectorEntity() {
    }

    public SourceConnectorEntity(UUID tenantId, SourceType type, String name) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.type = type;
        this.name = name;
        this.config = "{}";
        this.status = ConnectorStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public void recordSync(String newCursorJson) {
        this.syncCursor = newCursorJson;
        this.lastSyncAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public SourceType getType() { return type; }
    public String getName() { return name; }
    public String getConfig() { return config; }
    public String getSecretRef() { return secretRef; }
    public String getSyncSchedule() { return syncSchedule; }
    public String getSyncCursor() { return syncCursor; }
    public ConnectorStatus getStatus() { return status; }
    public Instant getLastSyncAt() { return lastSyncAt; }
    public Instant getCreatedAt() { return createdAt; }
}
