package com.avionics_systems.migration.service;

import com.avionics_systems.migration.entity.EntityStatus;
import com.avionics_systems.migration.entity.MigrationJob;
import com.avionics_systems.migration.repository.EntityStatusRepository;
import com.avionics_systems.migration.repository.MigrationJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PostMigrationVerificationService {

    private final MigrationJobRepository jobRepository;
    private final EntityStatusRepository entityStatusRepository;
    private final MigrationIssueResultService issueResultService;
    private final MigrationAttachmentResultService attachmentResultService;

    @Transactional(readOnly = true)
    public Map<String, Object> verify(UUID jobId) {
        MigrationJob job = jobRepository.findById(jobId).orElse(null);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("jobId", jobId.toString());
        if (job == null) {
            report.put("status", "NOT_FOUND");
            return report;
        }

        var issues = issueResultService.getByJob(jobId);
        var attachments = attachmentResultService.getByJob(jobId);
        List<EntityStatus> failed = entityStatusRepository.findFailedEntities(jobId);

        long issueSuccess = issues.stream().filter(i -> "SUCCESS".equals(i.getStatus())).count();
        long issueFailed = issues.stream().filter(i -> "FAILED".equals(i.getStatus())).count();
        long attSuccess = attachments.stream().filter(a -> "SUCCESS".equals(a.getStatus())).count();
        long attFailed = attachments.stream().filter(a -> "FAILED".equals(a.getStatus())).count();

        List<String> checks = new ArrayList<>();
        if (issueFailed == 0 && failed.isEmpty()) {
            checks.add("PASS: No failed issues");
        } else {
            checks.add("WARN: " + issueFailed + " issue failures, " + failed.size() + " entity failures");
        }
        if (job.getJobStatus() != null && "COMPLETED".equals(job.getJobStatus())) {
            checks.add("PASS: Job completed");
        } else {
            checks.add("FAIL: Job status is " + job.getJobStatus());
        }
        if (attFailed == 0 || attachments.isEmpty()) {
            checks.add("PASS: Attachments OK or N/A");
        } else {
            checks.add("WARN: " + attFailed + " attachment failures");
        }

        String overall = checks.stream().anyMatch(c -> c.startsWith("FAIL")) ? "FAILED"
                : checks.stream().anyMatch(c -> c.startsWith("WARN")) ? "WARN" : "PASSED";

        report.put("status", overall);
        report.put("jobStatus", job.getJobStatus());
        report.put("issueSuccess", issueSuccess);
        report.put("issueFailed", issueFailed);
        report.put("attachmentSuccess", attSuccess);
        report.put("attachmentFailed", attFailed);
        report.put("entityFailures", failed.size());
        report.put("checks", checks);
        report.put("verifiedAt", java.time.Instant.now().toString());
        return report;
    }
}
