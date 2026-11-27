package com.akiip.ingestion.adapter.out.persistence;

import com.akiip.ingestion.domain.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SourceConnectorRepository extends JpaRepository<SourceConnectorEntity, UUID> {

    List<SourceConnectorEntity> findByTenantId(UUID tenantId);

    Optional<SourceConnectorEntity> findByTenantIdAndType(UUID tenantId, SourceType type);
}
