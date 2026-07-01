package com.jira.migration.repository;

import com.jira.migration.entity.MigrationFileUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MigrationFileUploadRepository extends JpaRepository<MigrationFileUpload, UUID> {

    Optional<MigrationFileUpload> findFirstByWizardSessionIdOrderByCreatedAtDesc(UUID wizardSessionId);

    Optional<MigrationFileUpload> findFirstByMigrationJobId(UUID migrationJobId);
}
