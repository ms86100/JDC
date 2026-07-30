package com.avionics_systems.migration.service;

import com.avionics_systems.migration.entity.MigrationIssueResult;
import com.avionics_systems.migration.persister.IssuePersisterHandler;
import com.avionics_systems.migration.repository.MigrationIssueResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MigrationIssueResultService {

    private final MigrationIssueResultRepository issueResultRepository;

    @Transactional
    public void recordSuccess(
            UUID jobId,
            String sourceKey,
            IssuePersisterHandler.IssuePersisterResult result,
            Integer rowNumber) {
        issueResultRepository.save(MigrationIssueResult.builder()
                .jobId(jobId)
                .sourceIssueKey(sourceKey)
                .targetIssueId(result.getIssueId())
                .targetIssueKey(result.getIssueKey())
                .rowNumber(rowNumber)
                .status("SUCCESS")
                .build());
    }

    @Transactional
    public void recordFailure(UUID jobId, String sourceKey, String error, Integer rowNumber) {
        issueResultRepository.save(MigrationIssueResult.builder()
                .jobId(jobId)
                .sourceIssueKey(sourceKey)
                .rowNumber(rowNumber)
                .status("FAILED")
                .errorMessage(error)
                .build());
    }

    @Transactional(readOnly = true)
    public List<MigrationIssueResult> getByJob(UUID jobId) {
        return issueResultRepository.findByJobIdOrderByRowNumberAsc(jobId);
    }
}
