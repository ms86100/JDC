package com.avionics_systems.migration.repository;

import com.avionics_systems.migration.entity.MigrationAuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MigrationAuditRepository extends JpaRepository<MigrationAuditEntry, UUID> {

    List<MigrationAuditEntry> findByJobIdOrderByPerformedAtAsc(UUID jobId);
}
