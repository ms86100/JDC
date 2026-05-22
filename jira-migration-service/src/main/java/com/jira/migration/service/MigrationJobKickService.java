package com.jira.migration.service;

import com.jira.migration.async.ImportJobProcessor;
import com.jira.migration.entity.MigrationFileUpload;
import com.jira.migration.entity.MigrationJob;
import com.jira.migration.exception.EntityNotFoundException;
import com.jira.migration.repository.MigrationFileUploadRepository;
import com.jira.migration.repository.MigrationJobRepository;
import com.jira.migration.repository.WizardSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Starts processing for jobs stuck in PENDING (e.g. async import scheduled before TX commit).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationJobKickService {

    private final MigrationJobRepository migrationJobRepository;
    private final MigrationFileUploadRepository fileUploadRepository;
    private final WizardSessionRepository wizardSessionRepository;
    private final ImportJobProcessor importJobProcessor;
    private final MigrationJobLogService jobLogService;

    @Transactional(readOnly = true)
    public Map<String, Object> kickStalledJob(UUID jobId) {
        MigrationJob job = migrationJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationJob", jobId.toString()));

        String status = job.getJobStatus();
        if (!"PENDING".equals(status)) {
            return Map.of(
                    "jobId", jobId.toString(),
                    "kicked", false,
                    "message", "Job is already " + status);
        }

        MigrationFileUpload upload = fileUploadRepository.findFirstByMigrationJobId(jobId)
                .or(() -> wizardSessionRepository.findFirstByMigrationJobId(jobId)
                        .flatMap(s -> fileUploadRepository.findFirstByWizardSessionIdOrderByCreatedAtDesc(s.getId())))
                .orElseThrow(() -> new EntityNotFoundException("MigrationFileUpload", "jobId=" + jobId));

        byte[] fileContent = upload.getFileContent();
        if (fileContent == null || fileContent.length == 0) {
            throw new IllegalStateException("No file content stored for job " + jobId);
        }

        Map<String, Object> options = job.getOptions() != null ? new LinkedHashMap<>(job.getOptions()) : new LinkedHashMap<>();
        UUID userId = job.getInitiatedBy() != null ? job.getInitiatedBy() : UUID.fromString("00000000-0000-0000-0000-000000000001");
        String fileName = upload.getFileName() != null ? upload.getFileName() : "import.csv";
        String importSource = job.getImportSource() != null ? job.getImportSource() : "csv";

        jobLogService.appendLog(jobId, "INFO", "Manually kicking stalled PENDING job into import worker");

        if ("JIRA_DC".equalsIgnoreCase(importSource) || "ISSUE_XML".equalsIgnoreCase(importSource)) {
            importJobProcessor.processJiraDcImport(jobId, fileContent, fileName, options, userId);
        } else {
            importJobProcessor.processSpreadsheetImport(jobId, fileContent, fileName, null, options, userId);
        }

        log.info("Kicked stalled job {} into async processor", jobId);
        return Map.of(
                "jobId", jobId.toString(),
                "kicked", true,
                "message", "Import worker started");
    }
}
