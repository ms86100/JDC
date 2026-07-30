package com.avionics_systems.migration.service;

import com.avionics_systems.migration.entity.MigrationAttachmentResult;
import com.avionics_systems.migration.repository.MigrationAttachmentResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MigrationAttachmentResultService {

    private final MigrationAttachmentResultRepository repository;

    @Transactional
    public void recordSuccess(UUID jobId, String sourceIssueKey, UUID targetIssueId,
                              String fileName, String checksum) {
        repository.save(MigrationAttachmentResult.builder()
                .jobId(jobId)
                .sourceIssueKey(sourceIssueKey)
                .targetIssueId(targetIssueId)
                .fileName(fileName)
                .checksum(checksum)
                .status("SUCCESS")
                .build());
    }

    @Transactional
    public void recordFailure(UUID jobId, String sourceIssueKey, String fileName, String error) {
        repository.save(MigrationAttachmentResult.builder()
                .jobId(jobId)
                .sourceIssueKey(sourceIssueKey)
                .fileName(fileName)
                .status("FAILED")
                .errorMessage(error)
                .build());
    }

    @Transactional(readOnly = true)
    public List<MigrationAttachmentResult> getByJob(UUID jobId) {
        return repository.findByJobIdOrderByCreatedAtAsc(jobId);
    }
}
