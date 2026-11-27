package com.akiip.ingestion.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The embedding (meaning as 1024 numbers) of one chunk.
 * Mapped to pgvector's VECTOR(1024) column through the hibernate-vector module.
 */
@Entity
@Table(name = "chunk_embedding")
public class ChunkEmbeddingEntity {

    public static final int DIMENSIONS = 1024;

    @Id
    @Column(name = "chunk_id")
    private UUID chunkId;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = DIMENSIONS)
    @Column(nullable = false)
    private float[] embedding;

    @Column(nullable = false)
    private String model;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChunkEmbeddingEntity() {
    }

    public ChunkEmbeddingEntity(UUID chunkId, float[] embedding, String model) {
        Objects.requireNonNull(embedding, "embedding");
        if (embedding.length != DIMENSIONS) {
            throw new IllegalArgumentException(
                    "Expected " + DIMENSIONS + " dimensions but got " + embedding.length);
        }
        this.chunkId = chunkId;
        this.embedding = embedding;
        this.model = model;
        this.createdAt = Instant.now();
    }

    /** pgvector text format, e.g. "[0.1,0.2,0.3]". Used to pass a query vector to native SQL. */
    public static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 8).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }

    public UUID getChunkId() { return chunkId; }
    public float[] getEmbedding() { return embedding; }
    public String getModel() { return model; }
    public Instant getCreatedAt() { return createdAt; }
}
