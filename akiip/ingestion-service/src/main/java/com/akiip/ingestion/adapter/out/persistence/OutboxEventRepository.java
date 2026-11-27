package com.akiip.ingestion.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    /** What the outbox relay (Day 6) will poll: oldest unpublished events first. */
    List<OutboxEventEntity> findTop100ByPublishedAtIsNullOrderByIdAsc();
}
