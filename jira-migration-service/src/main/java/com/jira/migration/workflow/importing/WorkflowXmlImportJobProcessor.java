package com.jira.migration.workflow.importing;

import com.jira.migration.entity.MigrationJob;
import com.jira.migration.websocket.dto.ImportCompleteNotification;
import com.jira.migration.websocket.dto.MigrationError;
import com.jira.migration.repository.MigrationJobRepository;
import com.jira.migration.service.MigrationProgressNotifier;
import com.jira.migration.service.MigrationService;
import com.jira.migration.service.TargetProjectValidator;
import com.jira.migration.websocket.MigrationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowXmlImportJobProcessor {

    private final MigrationJobRepository migrationJobRepository;
    private final WorkflowXmlImportService workflowXmlImportService;
    private final MigrationService migrationService;
    private final MigrationProgressNotifier progressNotifier;
    private final TargetProjectValidator targetProjectValidator;
    private final MigrationWebSocketHandler webSocketHandler;

    public MigrationJob createJob(UUID projectId, UUID userId, boolean stubDownstream, boolean makeDefault) {
        if (projectId != null) {
            targetProjectValidator.assertProjectExists(projectId);
        }
        MigrationJob job = MigrationJob.builder()
                .jobType("IMPORT")
                .jobStatus("PENDING")
                .importSource("WORKFLOW_XML")
                .targetProjectId(projectId)
                .initiatedBy(userId)
                .totalEntities(4)
                .processedEntities(0)
                .failedEntities(0)
                .progressPercentage(0.0)
                .canRollback(true)
                .options(Map.of(
                        "stubDownstream", stubDownstream,
                        "makeDefault", makeDefault
                ))
                .build();
        return migrationJobRepository.save(job);
    }

    @Async("migrationTaskExecutor")
    public void processAsync(UUID jobId, String workflowXml, String schemeXml, UUID projectId,
                             UUID userId, boolean stubDownstream, boolean makeDefault) {
        String userIdStr = userId != null ? userId.toString() : "anonymous";
        try {
            migrationService.markJobStarted(jobId);
            progressNotifier.notifyProgress(jobId, userIdStr, 0, 4, 0, "VALIDATING", "WORKFLOW",
                    "Parsing and validating workflow descriptor XML");

            Map<String, Object> validation = workflowXmlImportService.validateOnly(workflowXml, schemeXml);
            if (Boolean.FALSE.equals(validation.get("valid"))) {
                throw new IllegalStateException("Validation failed: " + validation.get("errors"));
            }

            progressNotifier.notifyProgress(jobId, userIdStr, 1, 4, 0, "VALIDATING", "WORKFLOW",
                    "Workflow validation passed");

            progressNotifier.notifyProgress(jobId, userIdStr, 2, 4, 0, "IMPORTING", "WORKFLOW",
                    stubDownstream ? "Recording import locally (stub)" : "Pushing to workflow-service");

            Map<String, Object> result = workflowXmlImportService.importWorkflow(
                    workflowXml, schemeXml, jobId, projectId, userId, stubDownstream, makeDefault);

            progressNotifier.notifyProgress(jobId, userIdStr, 4, 4, 0, "COMPLETED", "WORKFLOW",
                    "Workflow import completed: " + result.get("targetWorkflowId"));

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("workflowXmlImport", result);
            metadata.put("importId", result.get("importId"));
            metadata.put("targetWorkflowId", result.get("targetWorkflowId"));
            metadata.put("stages", Map.of(
                    "VALIDATING", Map.of("completed", 1, "total", 1),
                    "IMPORTING", Map.of("completed", 1, "total", 1)
            ));
            migrationService.markJobCompleted(jobId, metadata);

            ImportCompleteNotification notification = ImportCompleteNotification.builder()
                    .jobId(jobId.toString())
                    .status("COMPLETED")
                    .successCount(1)
                    .failedCount(0)
                    .completedAt(Instant.now())
                    .summary(ImportCompleteNotification.ImportSummary.builder()
                            .totalProcessed(1)
                            .totalFailed(0)
                            .build())
                    .build();
            webSocketHandler.sendJobCompleted(jobId.toString(), userIdStr, notification);

        } catch (Exception e) {
            log.error("Workflow XML job {} failed: {}", jobId, e.getMessage(), e);
            migrationService.markJobFailed(jobId, e.getMessage(), Map.of("exception", e.getClass().getSimpleName()));
            progressNotifier.notifyProgress(jobId, userIdStr, 0, 4, 1, "FAILED", "WORKFLOW",
                    "Import failed: " + e.getMessage());
            webSocketHandler.sendErrorNotification(jobId.toString(), userIdStr, MigrationError.builder()
                    .jobId(jobId.toString())
                    .errorCode("WORKFLOW_XML_IMPORT_FAILED")
                    .errorMessage(e.getMessage())
                    .timestamp(Instant.now())
                    .severity("CRITICAL")
                    .build());
        }
    }
}
