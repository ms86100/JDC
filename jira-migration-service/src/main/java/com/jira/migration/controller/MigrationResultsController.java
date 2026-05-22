package com.jira.migration.controller;

import com.jira.migration.batch.DeadLetterQueueService;
import com.jira.migration.dc.JiraDcStagingService;
import com.jira.migration.entity.MigrationAuditEntry;
import com.jira.migration.entity.MigrationIssueResult;
import com.jira.migration.service.MigrationAuditPersistenceService;
import com.jira.migration.entity.MigrationAttachmentResult;
import com.jira.migration.service.MigrationAttachmentResultService;
import com.jira.migration.service.MigrationIssueResultService;
import com.jira.migration.service.MigrationJobLogService;
import com.jira.migration.service.MigrationJobKickService;
import com.jira.migration.service.MigrationJobReindexService;
import com.jira.migration.service.PostMigrationVerificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/migration/jobs/{jobId}")
@RequiredArgsConstructor
@Tag(name = "Migration Job Results", description = "Issue results, staging, audit, logs, DLQ for a job")
public class MigrationResultsController {

    private final MigrationIssueResultService issueResultService;
    private final JiraDcStagingService stagingService;
    private final MigrationAuditPersistenceService auditService;
    private final MigrationJobLogService jobLogService;
    private final DeadLetterQueueService deadLetterQueueService;
    private final MigrationAttachmentResultService attachmentResultService;
    private final PostMigrationVerificationService verificationService;
    private final MigrationJobReindexService reindexService;
    private final MigrationJobKickService kickService;

    @PostMapping("/kick")
    public ResponseEntity<Map<String, Object>> kickStalledJob(@PathVariable UUID jobId) {
        return ResponseEntity.accepted().body(kickService.kickStalledJob(jobId));
    }

    @GetMapping("/issue-results")
    public ResponseEntity<List<MigrationIssueResult>> issueResults(@PathVariable UUID jobId) {
        return ResponseEntity.ok(issueResultService.getByJob(jobId));
    }

    @GetMapping("/attachment-results")
    public ResponseEntity<List<MigrationAttachmentResult>> attachmentResults(@PathVariable UUID jobId) {
        return ResponseEntity.ok(attachmentResultService.getByJob(jobId));
    }

    @GetMapping("/verification")
    public ResponseEntity<Map<String, Object>> verification(@PathVariable UUID jobId) {
        return ResponseEntity.ok(verificationService.verify(jobId));
    }

    @PostMapping("/reindex")
    public ResponseEntity<Map<String, Object>> triggerReindex(
            @PathVariable UUID jobId,
            @RequestParam(required = false) List<String> entityTypes) {
        reindexService.triggerReindex(jobId, entityTypes);
        return ResponseEntity.accepted().body(Map.of(
                "jobId", jobId.toString(),
                "status", "STARTED",
                "message", "Reindex started; poll GET /reindex for status"
        ));
    }

    @GetMapping("/reindex")
    public ResponseEntity<Map<String, Object>> reindexStatus(@PathVariable UUID jobId) {
        return ResponseEntity.ok(reindexService.getReindexStatus(jobId));
    }

    @GetMapping("/config-import-summary")
    public ResponseEntity<Map<String, Object>> configImportSummary(@PathVariable UUID jobId) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("jobId", jobId.toString());
        var entities = issueResultService.getByJob(jobId);
        body.put("issueResults", entities.size());
        body.put("attachmentResults", attachmentResultService.getByJob(jobId).size());
        body.put("reindex", reindexService.getReindexStatus(jobId));
        body.put("verification", verificationService.verify(jobId));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/staging-summary")
    public ResponseEntity<Map<String, Object>> stagingSummary(@PathVariable UUID jobId) {
        return ResponseEntity.ok(stagingService.summarizeByJob(jobId));
    }

    @GetMapping("/audit-trail")
    public ResponseEntity<List<MigrationAuditEntry>> auditTrail(@PathVariable UUID jobId) {
        return ResponseEntity.ok(auditService.getJobTrail(jobId));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<Map<String, Object>>> logs(@PathVariable UUID jobId) {
        return ResponseEntity.ok(jobLogService.getRecentLogs(jobId));
    }

    @GetMapping("/dlq")
    public ResponseEntity<List<DeadLetterQueueService.FailedOperation>> dlqForJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(deadLetterQueueService.getByJobId(jobId.toString()));
    }

    @PostMapping("/dlq/{dlqId}/retry")
    public ResponseEntity<DeadLetterQueueService.RetryResult> retryDlq(
            @PathVariable UUID jobId,
            @PathVariable String dlqId) {
        return ResponseEntity.ok(deadLetterQueueService.retry(dlqId));
    }
}
