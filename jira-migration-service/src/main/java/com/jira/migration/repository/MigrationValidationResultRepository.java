package com.jira.migration.repository;

import com.jira.migration.entity.MigrationValidationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MigrationValidationResultRepository extends JpaRepository<MigrationValidationResult, UUID> {

    List<MigrationValidationResult> findByJobIdOrderByRowNumberAsc(UUID jobId);

    List<MigrationValidationResult> findByWizardSessionIdOrderByRowNumberAsc(UUID sessionId);

    void deleteByJobId(UUID jobId);

    void deleteByWizardSessionId(UUID wizardSessionId);
}
