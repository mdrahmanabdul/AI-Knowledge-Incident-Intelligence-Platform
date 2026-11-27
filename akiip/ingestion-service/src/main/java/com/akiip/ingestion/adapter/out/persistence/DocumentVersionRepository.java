package com.akiip.ingestion.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersionEntity, UUID> {

    /** Duplicate check: same document, same SHA-256 => nothing changed, skip processing. */
    boolean existsByDocumentIdAndContentHash(UUID documentId, String contentHash);

    Optional<DocumentVersionEntity> findFirstByDocumentIdOrderByVersionNoDesc(UUID documentId);

    List<DocumentVersionEntity> findByDocumentIdOrderByVersionNoAsc(UUID documentId);
}
