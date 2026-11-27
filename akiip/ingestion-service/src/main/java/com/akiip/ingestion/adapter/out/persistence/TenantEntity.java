package com.akiip.ingestion.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant")
public class TenantEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TenantEntity() {
        // for JPA
    }

    public TenantEntity(String name, String slug) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.slug = slug;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
