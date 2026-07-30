package com.avionics_systems.migration.repository;

import com.avionics_systems.migration.entity.MigrationWorkflowImport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MigrationWorkflowImportRepository extends JpaRepository<MigrationWorkflowImport, UUID> {
    List<MigrationWorkflowImport> findByJobIdOrderByCreatedAtDesc(UUID jobId);
    Optional<MigrationWorkflowImport> findFirstByWorkflowNameOrderByCreatedAtDesc(String workflowName);
}
