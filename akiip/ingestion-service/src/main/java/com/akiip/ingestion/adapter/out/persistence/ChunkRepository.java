package com.akiip.ingestion.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChunkRepository extends JpaRepository<ChunkEntity, UUID> {

    List<ChunkEntity> findByDocumentVersionIdOrderByOrdinalAsc(UUID documentVersionId);

    long countByDocumentIdAndLatestTrue(UUID documentId);
}
