package com.akiip.ingestion.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Local mirror of a Keycloak user. Passwords never live here. */
@Entity
@Table(name = "app_user")
public class AppUserEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** The JWT "sub" claim issued by Keycloak. */
    @Column(name = "idp_subject", nullable = false, unique = true)
    private String idpSubject;

    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    protected AppUserEntity() {
    }

    public AppUserEntity(UUID tenantId, String idpSubject, String email, String displayName) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.idpSubject = idpSubject;
        this.email = email;
        this.displayName = displayName;
        this.createdAt = Instant.now();
    }

    public void markSeen() { this.lastSeenAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getIdpSubject() { return idpSubject; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
}
