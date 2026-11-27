package com.akiip.ingestion.adapter.out.persistence;

import com.akiip.ingestion.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IngestionJobRepository extends JpaRepository<IngestionJobEntity, UUID> {

    List<IngestionJobEntity> findBySourceIdOrderByCreatedAtDesc(UUID sourceId);

    List<IngestionJobEntity> findByStatusIn(List<JobStatus> statuses);
}
