package com.akiip.ingestion.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChunkEmbeddingRepository extends JpaRepository<ChunkEmbeddingEntity, UUID> {

    /**
     * Nearest-neighbour search: the k chunks whose embedding is closest to the query vector.
     * {@code <=>} is pgvector's cosine-distance operator (smaller = more similar),
     * served by the HNSW index created in V1__init.sql.
     *
     * @param queryVector pgvector literal, see {@link ChunkEmbeddingEntity#toVectorLiteral(float[])}
     */
    @Query(value = """
            SELECT ce.chunk_id
            FROM chunk_embedding ce
            ORDER BY ce.embedding <=> cast(:queryVector AS vector)
            LIMIT :k
            """, nativeQuery = true)
    List<UUID> findNearestChunkIds(@Param("queryVector") String queryVector, @Param("k") int k);
}
