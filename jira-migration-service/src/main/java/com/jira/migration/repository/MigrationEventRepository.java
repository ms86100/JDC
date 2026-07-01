package com.jira.migration.repository;

import com.jira.migration.entity.MigrationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MigrationEventRepository extends JpaRepository<MigrationEvent, UUID> {

    List<MigrationEvent> findTop50ByStatusOrderByCreatedAtAsc(String status);
}
