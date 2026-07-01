package com.jira.migration.repository;

import com.jira.migration.entity.MigrationAuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MigrationAuditRepository extends JpaRepository<MigrationAuditEntry, UUID> {

    List<MigrationAuditEntry> findByJobIdOrderByPerformedAtAsc(UUID jobId);
}
