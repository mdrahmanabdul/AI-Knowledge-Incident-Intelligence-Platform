package com.akiip.ingestion.adapter.out.persistence;

import com.akiip.ingestion.domain.DocType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    /** "Have we seen this source document before?" (unique key in the schema). */
    Optional<DocumentEntity> findByTenantIdAndSourceIdAndExternalId(UUID tenantId, UUID sourceId, String externalId);

    List<DocumentEntity> findByTenantIdAndDocType(UUID tenantId, DocType docType);
}
