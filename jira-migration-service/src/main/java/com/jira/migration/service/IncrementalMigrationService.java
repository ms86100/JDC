package com.jira.migration.service;

import com.jira.migration.entity.MigrationIssueResult;
import com.jira.migration.repository.MigrationIssueResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncrementalMigrationService {

    private final MigrationIssueResultRepository issueResultRepository;

    public boolean shouldSkipIssue(UUID jobId, String sourceIssueKey) {
        if (sourceIssueKey == null || sourceIssueKey.isBlank()) {
            return false;
        }
        if (jobId != null) {
            return issueResultRepository.existsByJobIdAndSourceIssueKeyAndStatus(
                    jobId, sourceIssueKey, "SUCCESS");
        }
        return issueResultRepository.existsBySourceIssueKeyAndStatus(sourceIssueKey, "SUCCESS");
    }

    public Optional<MigrationIssueResult> priorSuccess(UUID jobId, String sourceIssueKey) {
        if (sourceIssueKey == null || sourceIssueKey.isBlank()) {
            return Optional.empty();
        }
        if (jobId != null) {
            return issueResultRepository.findFirstByJobIdAndSourceIssueKeyAndStatusOrderByCreatedAtDesc(
                    jobId, sourceIssueKey, "SUCCESS");
        }
        return issueResultRepository.findFirstBySourceIssueKeyAndStatusOrderByCreatedAtDesc(
                sourceIssueKey, "SUCCESS");
    }
}
