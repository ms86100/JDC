package com.jira.migration.repository;

import com.jira.migration.entity.MigrationIssueResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MigrationIssueResultRepository extends JpaRepository<MigrationIssueResult, UUID> {

    List<MigrationIssueResult> findByJobIdOrderByRowNumberAsc(UUID jobId);

    boolean existsBySourceIssueKeyAndStatus(String sourceIssueKey, String status);

    boolean existsByJobIdAndSourceIssueKeyAndStatus(UUID jobId, String sourceIssueKey, String status);

    Optional<MigrationIssueResult> findFirstBySourceIssueKeyAndStatusOrderByCreatedAtDesc(
            String sourceIssueKey, String status);

    Optional<MigrationIssueResult> findFirstByJobIdAndSourceIssueKeyAndStatusOrderByCreatedAtDesc(
            UUID jobId, String sourceIssueKey, String status);
}
