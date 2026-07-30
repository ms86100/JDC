package com.avionics_systems.migration.async;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.migration.dto.ImportResultResponse;
import com.avionics_systems.migration.dto.JobProgressResponse;
import com.avionics_systems.migration.dto.StartMigrationRequest;
import com.avionics_systems.migration.dto.ValidationResult;
import com.avionics_systems.migration.entity.*;
import com.avionics_systems.migration.parser.CsvParser;
import com.avionics_systems.migration.parser.ImportSpreadsheetParser;
import com.avionics_systems.migration.parser.LegacyDcEntityMapper;
import com.avionics_systems.migration.parser.LegacyDcXmlParser;
import com.avionics_systems.migration.parser.ValidationEngine;
import com.avionics_systems.migration.persister.*;
import com.avionics_systems.migration.repository.*;
import com.avionics_systems.migration.service.CsvFieldMappingService;
import com.avionics_systems.migration.service.FieldDefaultValueService;
import com.avionics_systems.migration.service.ChunkedAttachmentUploadService;
import com.avionics_systems.migration.service.MigrationAttachmentResultService;
import com.avionics_systems.migration.service.MigrationAuditPersistenceService;
import com.avionics_systems.migration.service.IncrementalMigrationService;
import com.avionics_systems.migration.service.MigrationIssueResultService;
import com.avionics_systems.migration.service.MigrationJobLogService;
import com.avionics_systems.migration.service.MigrationEventPublisher;
import com.avionics_systems.migration.service.MigrationJobControlService;
import com.avionics_systems.migration.service.MigrationJobReindexService;
import com.avionics_systems.migration.service.ProjectImportOrchestrator;
import com.avionics_systems.migration.service.VirusScanService;
import com.avionics_systems.migration.dc.LegacyDcAttachmentBundleResolver;
import com.avionics_systems.migration.dc.LegacyDcCsvAttachmentResolver;
import com.avionics_systems.migration.dc.LegacyDcChangeHistoryReplayer;
import com.avionics_systems.migration.jiradc.JiraDcApiImportOrchestrator;
import com.avionics_systems.migration.jiradc.JiraDcConnectionConfig;
import com.avionics_systems.migration.dc.LegacyDcImportOrchestrator;
import com.avionics_systems.migration.dc.LegacyDcAcSignoffEvaluator;
import com.avionics_systems.migration.dc.LegacyDcImportSlaProofBuilder;
import com.avionics_systems.migration.dc.LegacyDcParitySummaryBuilder;
import com.avionics_systems.migration.dc.LegacyDcIssueIdRegistry;
import com.avionics_systems.migration.dc.LegacyDcReferenceCatalog;
import com.avionics_systems.migration.persister.LabelPersisterHandler;
import com.avionics_systems.migration.service.MigrationRollbackService;
import com.avionics_systems.migration.service.MigrationService;
import com.avionics_systems.migration.service.MigrationWorkflowStatusApplier;
import com.avionics_systems.migration.service.clients.IssueServiceClient;
import com.avionics_systems.migration.service.clients.ProjectServiceClient;
import com.avionics_systems.migration.service.OptionMappingService;
import com.avionics_systems.migration.service.ProjectMappingSetupService;
import com.avionics_systems.migration.service.PollingFallbackService;
import com.avionics_systems.migration.service.UserDirectoryMappingService;
import com.avionics_systems.migration.service.WorkflowStatusMappingService;
import com.avionics_systems.migration.websocket.MigrationWebSocketHandler;
import com.avionics_systems.migration.websocket.dto.ImportCompleteNotification;
import com.avionics_systems.migration.websocket.dto.JobProgressUpdate;
import com.avionics_systems.migration.websocket.dto.MigrationError;
import com.avionics_systems.migration.websocket.dto.ValidationUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImportJobProcessor {

    @Value("${app.import.default-filename:import.csv}")
    private String defaultImportFilename;

    @Value("${app.import.default-project-key:DEMO}")
    private String defaultProjectKey;

    @Value("${app.import.reindex-entity-types:ISSUE,COMMENT,PROJECT}")
    private String reindexEntityTypesStr;

    @Value("${app.import.parallel-entity-types:Comment,Attachment,Worklog}")
    private String parallelEntityTypesStr;

    @Value("${app.import.dc-processing-order:Project,IssueType,Status,Priority,Resolution,Component,Version,Label,User,Group,Issue,SubTask,Comment,Attachment,Worklog,IssueLink,History,Watcher,Vote,CustomField,PluginEntity,Workflow}")
    private String dcProcessingOrderStr;

    @Value("${app.import.project-entity-types:PROJECT,ISSUE_TYPE,STATUS,PRIORITY,RESOLUTION,COMPONENT,VERSION,WORKFLOW,SCREEN,FIELD_CONFIG,PERMISSION_SCHEME,NOTIFICATION_SCHEME,ISSUE,COMMENT,ATTACHMENT,WORKLOG,LABEL,CUSTOM_FIELD}")
    private String projectEntityTypesStr;

    private final MigrationService migrationService;
    private final CsvParser csvParser;
    private final ImportSpreadsheetParser importSpreadsheetParser;
    private final LegacyDcXmlParser xmlParser;
    private final ValidationEngine validationEngine;
    private final EntityStatusRepository entityStatusRepository;
    private final ProjectMappingRepository projectMappingRepository;
    private final UserMappingRepository userMappingRepository;
    private final ObjectMapper objectMapper;
    private final MigrationWebSocketHandler webSocketHandler;
    private final PollingFallbackService pollingFallbackService;

    // Persister handlers for real data persistence
    private final IssuePersisterHandler issuePersisterHandler;
    private final IssueLinkPersisterHandler issueLinkPersisterHandler;
    private final MigrationIssueResultService migrationIssueResultService;
    private final MigrationAuditPersistenceService migrationAuditPersistenceService;
    private final ProjectPersisterHandler projectPersisterHandler;
    private final UserPersisterHandler userPersisterHandler;
    private final SprintPersisterHandler sprintPersisterHandler;
    private final CsvFieldMappingService csvFieldMappingService;
    private final FieldDefaultValueService fieldDefaultValueService;
    private final OptionMappingService optionMappingService;
    private final WorkflowStatusMappingService workflowStatusMappingService;
    private final UserDirectoryMappingService userDirectoryMappingService;
    private final ProjectMappingSetupService projectMappingSetupService;
    private final MigrationJobRepository migrationJobRepository;
    private final CommentPersisterHandler commentPersisterHandler;
    private final AttachmentPersisterHandler attachmentPersisterHandler;
    private final WorklogPersisterHandler worklogPersisterHandler;
    private final MigrationAttachmentResultService migrationAttachmentResultService;
    private final MigrationRollbackService migrationRollbackService;
    private final LegacyDcImportOrchestrator legacyDcImportOrchestrator;
    private final LegacyDcAttachmentBundleResolver attachmentBundleResolver;
    private final LegacyDcChangeHistoryReplayer changeHistoryReplayer;
    private final MigrationWorkflowStatusApplier migrationWorkflowStatusApplier;
    private final IssueServiceClient issueServiceClient;
    private final ProjectImportOrchestrator projectImportOrchestrator;
    private final MigrationJobLogService migrationJobLogService;
    private final LabelPersisterHandler labelPersisterHandler;
    private final ComponentPersisterHandler componentPersisterHandler;
    private final VersionPersisterHandler versionPersisterHandler;
    private final CustomFieldPersisterHandler customFieldPersisterHandler;
    private final LegacyDcReferenceCatalog referenceCatalog;
    private final IncrementalMigrationService incrementalMigrationService;
    private final MigrationJobReindexService migrationJobReindexService;
    private final MigrationJobControlService migrationJobControlService;
    private final MigrationEventPublisher migrationEventPublisher;
    private final VirusScanService virusScanService;
    private final LegacyDcCsvAttachmentResolver csvAttachmentResolver;
    private final ProjectServiceClient projectServiceClient;
    private final JiraDcApiImportOrchestrator jiraDcApiImportOrchestrator;

    @Value("${app.import.progress-update-batch-size:50}")
    private int progressUpdateBatchSize;

    @Async("migrationTaskExecutor")
    public CompletableFuture<ImportResultResponse> processSpreadsheetImport(
            UUID jobId,
            byte[] fileContent,
            String fileName,
            UUID templateId,
            Map<String, Object> options,
            UUID userId) {
        return processCsvImport(jobId, fileContent, fileName, templateId, options, userId);
    }

    @Async("migrationTaskExecutor")
    public CompletableFuture<ImportResultResponse> processCsvImport(
            UUID jobId,
            byte[] fileContent,
            String fileName,
            UUID templateId,
            Map<String, Object> options,
            UUID userId) {

        log.info("Starting spreadsheet import job: {} file={}", jobId, fileName);
        String userIdStr = userId != null ? userId.toString() : "system";

        try {
            com.avionics_systems.migration.security.MigrationRequestContext.setUserId(userId);
            migrationService.markJobStarted(jobId);
            migrationAuditPersistenceService.log(jobId, "IMPORT_STARTED", "JOB", jobId.toString(), userId, Map.of());
            migrationEventPublisher.enqueue(jobId, "IMPORT_STARTED", Map.of("source", "CSV"));
            updateStageProgress(jobId, "PARSING", 0, 0);

            // Send initial progress via WebSocket
            sendProgressUpdate(jobId, userIdStr, 0, 0, 0, "PARSING", null);

            CsvParser.CsvParseResult parseResult = importSpreadsheetParser.parse(
                    fileContent,
                    fileName != null ? fileName : defaultImportFilename
            );

            log.info("Parsed {} rows from {}", parseResult.getTotalRows(), fileName);
            migrationService.setTotalEntities(jobId, parseResult.getTotalRows());

            MigrationJob job = migrationJobRepository.findById(jobId).orElse(null);
            Map<String, Object> jobOptions = job != null && job.getOptions() != null ? job.getOptions() : options;
            Object fieldMappingsOpt = jobOptions.get("fieldMappings");

            // Build row maps and apply field mappings
            List<Map<String, String>> csvRows = new ArrayList<>();
            for (int i = 0; i < parseResult.getDataRows().size(); i++) {
                csvRows.add(convertRowToMap(parseResult.getDataRows().get(i), parseResult.getHeaders()));
            }
            List<Map<String, String>> mappedRows = csvFieldMappingService.applyMappings(csvRows, fieldMappingsOpt);

            Map<String, Object> fieldDefaults = fieldDefaultValueService.parseDefaults(
                    jobOptions.get("fieldDefaults"));
            mappedRows = fieldDefaultValueService.applyDefaults(mappedRows, fieldDefaults);

            Map<String, Object> workflowMappings = job != null && job.getWorkflowStatusMappings() != null
                    ? job.getWorkflowStatusMappings()
                    : parseWorkflowMappings(jobOptions.get("workflowStatusMappings"));
            List<com.avionics_systems.migration.entity.OptionMapping> optionMappingsList =
                    optionMappingService.getForJob(jobId);
            mappedRows = applyOptionAndStatusMappings(jobId, mappedRows, workflowMappings, optionMappingsList);

            resolveAssigneeReporterUsers(mappedRows, jobId);

            UUID targetProjectId = job != null ? job.getTargetProjectId() : null;
            if (targetProjectId == null && options.get("targetProjectId") != null) {
                targetProjectId = UUID.fromString(options.get("targetProjectId").toString());
            }

            deriveProjectKeyForRows(mappedRows, targetProjectId);

            String csvImportProfile = stringOption(jobOptions, "csvImportProfile", "EXTERNAL");
            if ("LIGHTWEIGHT".equalsIgnoreCase(csvImportProfile)) {
                migrationJobLogService.appendLog(jobId, "INFO",
                        "CSV import profile LIGHTWEIGHT — attachment column import disabled (Legacy DC lightweight importer)");
            } else {
                validateCsvSingleTargetProject(mappedRows, targetProjectId, jobId);
            }

            projectMappingSetupService.ensureProjectMappings(jobId, targetProjectId, mappedRows);

            sendProgressUpdate(jobId, userIdStr, 0, parseResult.getTotalRows(), 0, "VALIDATING", null);

            boolean hasEntityTypeColumn = parseResult.getHeaders() != null
                    && Arrays.stream(parseResult.getHeaders())
                    .anyMatch(h -> "entity_type".equalsIgnoreCase(h.trim()));

            List<Map<String, Object>> projectRows = new ArrayList<>();
            List<Map<String, Object>> issueRows = new ArrayList<>();
            List<Map<String, Object>> commentRows = new ArrayList<>();
            List<Map<String, Object>> attachmentRows = new ArrayList<>();
            int validationFailures = 0;
            for (int i = 0; i < mappedRows.size(); i++) {
                Map<String, String> rowData = mappedRows.get(i);
                int rowNum = i + 2;
                String entityType = hasEntityTypeColumn
                        ? rowData.getOrDefault("entity_type", "ISSUE").toUpperCase(Locale.ROOT)
                        : (isProjectCsvRow(rowData)
                        ? "PROJECT"
                        : (hasNonBlank(rowData, "comment_body", "comment")
                        ? "COMMENT"
                        : (hasNonBlank(rowData, "attachment_path", "attachment_url")
                        || (hasNonBlank(rowData, "file_name", "filename")
                        && hasNonBlank(rowData, "issue_key", "issuekey"))
                        ? "ATTACHMENT" : "ISSUE")));

                if ("ATTACHMENT".equals(entityType)) {
                    Map<String, Object> att = new HashMap<>(rowData);
                    att.put("issueKey", rowData.getOrDefault("issue_key", rowData.get("issuekey")));
                    att.put("fileName", rowData.getOrDefault("file_name", rowData.get("filename")));
                    att.put("attachmentPath", rowData.getOrDefault("attachment_path", rowData.get("attachment_url")));
                    attachmentRows.add(att);
                    continue;
                }

                if ("COMMENT".equals(entityType)) {
                    Map<String, Object> commentData = new HashMap<>(rowData);
                    commentData.put("issueKey", rowData.getOrDefault("issue_key", rowData.get("issuekey")));
                    commentData.put("body", rowData.getOrDefault("comment_body", rowData.get("comment")));
                    commentData.put("author", rowData.getOrDefault("author", rowData.get("comment_author")));
                    commentRows.add(commentData);
                    continue;
                }

                if ("PROJECT".equals(entityType)) {
                    var projectValidation = validationEngine.validateRow(rowData, "PROJECT", rowNum);
                    if (!projectValidation.isValid()) {
                        recordFailure(jobId, "PROJECT", rowData.get("project_key"), rowNum,
                                "VALIDATION_ERROR", projectValidation.getErrors().get(0).getMessage(), null);
                        validationFailures++;
                        sendValidationError(jobId, userIdStr, rowNum, projectValidation.getErrors());
                        continue;
                    }
                    Map<String, Object> projectData = new HashMap<>();
                    projectData.put("projectKey", rowData.get("project_key"));
                    projectData.put("name", rowData.get("name"));
                    if (rowData.get("description") != null) {
                        projectData.put("description", rowData.get("description"));
                    }
                    projectRows.add(projectData);
                    continue;
                }

                var validationResult = validationEngine.validateRow(rowData, "ISSUE", rowNum);
                if (!validationResult.isValid()) {
                    recordFailure(jobId, "ISSUE", rowData.get("issue_key"), rowNum,
                            "VALIDATION_ERROR", validationResult.getErrors().get(0).getMessage(), null);
                    validationFailures++;
                    sendValidationError(jobId, userIdStr, rowNum, validationResult.getErrors());
                    continue;
                }
                String issueKey = rowData.getOrDefault("issue_key", rowData.getOrDefault("issuekey", "ROW-" + rowNum));
                Map<String, Object> issueData = csvFieldMappingService.buildIssueDataFromCsvRow(
                        rowData, issueKey, rowNum, targetProjectId);
                issueRows.add(issueData);
            }

            if (validationFailures > 0 && Boolean.TRUE.equals(jobOptions.get("blockOnValidationErrors"))) {
                migrationService.markJobFailed(jobId,
                        "Import blocked: " + validationFailures + " validation error(s). Fix data and retry.",
                        Map.of("validationFailures", validationFailures));
                sendJobFailed(jobId, userIdStr, "Validation blocked import");
                return CompletableFuture.failedFuture(
                        new IllegalStateException("Validation blocked import"));
            }

            sendProgressUpdate(jobId, userIdStr, 0, parseResult.getTotalRows(), validationFailures, "PROCESSING", null);

            // Phase 2: batch persist with hierarchy ordering (Epic -> Story -> Task -> Subtask)
            int processedCount = 0;
            int failedCount = validationFailures;

            if (!projectRows.isEmpty()) {
                updateStageProgress(jobId, "PROJECTS", 0, projectRows.size());
                for (Map<String, Object> projectData : projectRows) {
                    var projectResult = projectPersisterHandler.persistProject(projectData, jobId);
                    if (projectResult.isSuccess()) {
                        processedCount++;
                    } else {
                        failedCount++;
                    }
                }
                updateStageProgress(jobId, "PROJECTS", projectRows.size(), projectRows.size());
            }

            if (!issueRows.isEmpty()) {
                updateStageProgress(jobId, "ISSUES", 0, issueRows.size());
                var batchResult = issuePersisterHandler.batchPersistIssues(issueRows, jobId);
                processedCount = batchResult.getSuccessCount();
                failedCount += batchResult.getFailureCount();

                recordIssueResults(jobId, batchResult);
                persistIssueLinksPass(issueRows, jobId);
                updateStageProgress(jobId, "LINKS", issueRows.size(), issueRows.size());

                if (!"LIGHTWEIGHT".equalsIgnoreCase(csvImportProfile)) {
                    int colAtt = processCsvIssueColumnAttachments(issueRows, jobId, jobOptions);
                    processedCount += colAtt;
                }

                migrationService.updateJobProgress(jobId, processedCount, failedCount);
                sendProgressUpdate(jobId, userIdStr, processedCount + failedCount,
                        parseResult.getTotalRows(), failedCount, "PROCESSING", "ISSUE");
            }

            if (!commentRows.isEmpty()) {
                updateStageProgress(jobId, "COMMENTS", 0, commentRows.size());
                int commentOk = 0;
                int commentFail = 0;
                for (Map<String, Object> commentData : commentRows) {
                    try {
                        var result = commentPersisterHandler.persistComment(commentData, jobId);
                        if (result != null && result.isSuccess()) {
                            commentOk++;
                        } else {
                            commentFail++;
                        }
                    } catch (Exception e) {
                        commentFail++;
                    }
                }
                processedCount += commentOk;
                failedCount += commentFail;
                updateStageProgress(jobId, "COMMENTS", commentRows.size(), commentRows.size());
                migrationJobLogService.appendLog(jobId, "INFO",
                        "CSV comments: " + commentOk + " ok, " + commentFail + " failed");
                migrationService.updateJobProgress(jobId, processedCount, failedCount);
                sendProgressUpdate(jobId, userIdStr, processedCount + failedCount,
                        parseResult.getTotalRows(), failedCount, "PROCESSING", "COMMENT");
            }

            if (!attachmentRows.isEmpty() && !"LIGHTWEIGHT".equalsIgnoreCase(csvImportProfile)) {
                updateStageProgress(jobId, "ATTACHMENTS", 0, attachmentRows.size());
                int attOk = 0;
                int attFail = 0;
                for (Map<String, Object> attData : attachmentRows) {
                    try {
                        byte[] content = resolveCsvAttachmentContent(attData);
                        if (content.length == 0) {
                            attFail++;
                            continue;
                        }
                        String scan = virusScanService.scanBytes(content,
                                (String) attData.get("fileName"));
                        if ("INFECTED".equals(scan)) {
                            attFail++;
                            continue;
                        }
                        var result = attachmentPersisterHandler.persistAttachment(
                                attData, content, jobId);
                        if (result != null && result.isSuccess()) {
                            attOk++;
                        } else {
                            attFail++;
                        }
                    } catch (Exception e) {
                        attFail++;
                    }
                }
                processedCount += attOk;
                failedCount += attFail;
                updateStageProgress(jobId, "ATTACHMENTS", attachmentRows.size(), attachmentRows.size());
                migrationJobLogService.appendLog(jobId, "INFO",
                        "CSV attachments: " + attOk + " ok, " + attFail + " failed");
                migrationService.updateJobProgress(jobId, processedCount, failedCount);
            }

            // Final progress update
            sendProgressUpdate(jobId, userIdStr, processedCount + failedCount,
                    parseResult.getTotalRows(), failedCount, "COMPLETING", null);

            // Mark job completed and send notification
            MigrationJob completedJob = migrationJobRepository.findById(jobId).orElse(null);
            long durationMs = 0;
            if (completedJob != null && completedJob.getStartedAt() != null) {
                durationMs = java.time.Duration.between(
                        completedJob.getStartedAt(), java.time.LocalDateTime.now()).toMillis();
            }
            Map<String, Object> resultMetadata = new HashMap<>(Map.of(
                    "processed", processedCount,
                    "failed", failedCount,
                    "successRate", (processedCount + failedCount) > 0
                            ? (processedCount * 100.0 / (processedCount + failedCount)) : 0.0,
                    "durationMs", durationMs
            ));
            migrationJobRepository.findById(jobId).ifPresent(j -> {
                if (j.getResultMetadata() != null && j.getResultMetadata().get("stages") != null) {
                    resultMetadata.put("stages", j.getResultMetadata().get("stages"));
                }
            });
            if (processedCount == 0 && failedCount > 0) {
                String failMsg = "All " + failedCount + " entit"
                        + (failedCount == 1 ? "y" : "ies")
                        + " failed to import. Check Imported issues, job console logs, and issue-service health.";
                migrationJobLogService.appendLog(jobId, "ERROR", failMsg);
                migrationService.markJobFailed(jobId, failMsg, resultMetadata);
                sendJobFailed(jobId, userIdStr, failMsg);
                return CompletableFuture.completedFuture(migrationService.getImportResult(jobId));
            }

            migrationService.markJobCompleted(jobId, resultMetadata);
            migrationAuditPersistenceService.log(jobId, "IMPORT_COMPLETED", "JOB", jobId.toString(), userId,
                    Map.of("processed", processedCount, "failed", failedCount));

            // Send job completion notification
            sendJobCompleted(jobId, userIdStr, processedCount, failedCount);

            return CompletableFuture.completedFuture(migrationService.getImportResult(jobId));

        } catch (Exception e) {
            log.error("CSV import job failed: {}", e.getMessage(), e);
            migrationJobLogService.appendLog(jobId, "ERROR", "Import failed: " + e.getMessage());
            migrationService.markJobFailed(jobId, e.getMessage(), null);

            // Send job failure notification
            sendJobFailed(jobId, userIdStr, e.getMessage());

            return CompletableFuture.failedFuture(e);
        } finally {
            com.avionics_systems.migration.security.MigrationRequestContext.clear();
        }
    }

    @Async("migrationTaskExecutor")
    public CompletableFuture<ImportResultResponse> processLegacyDcImport(
            UUID jobId,
            byte[] fileContent,
            String fileName,
            Map<String, Object> options,
            UUID userId) {

        log.info("Starting Legacy DC XML import job: {}", jobId);
        String userIdStr = userId != null ? userId.toString() : "system";

        Path tempFile = null;
        LegacyDcImportOrchestrator.ResolvedInputs extractedForCleanup = null;
        try {
            com.avionics_systems.migration.security.MigrationRequestContext.setUserId(userId);
            migrationService.markJobStarted(jobId);
            migrationAuditPersistenceService.log(jobId, "IMPORT_STARTED", "JOB", jobId.toString(), userId,
                    Map.of("source", "LEGACY_DC"));
            migrationEventPublisher.enqueue(jobId, "IMPORT_STARTED", Map.of("source", "LEGACY_DC"));
            sendProgressUpdate(jobId, userIdStr, 0, 0, 0, "PARSING", null);

            if (options != null && options.get("xmlPath") != null) {
                tempFile = Path.of(options.get("xmlPath").toString());
            } else {
                tempFile = Files.createTempFile("import-", fileName != null ? "-" + fileName : ".xml");
                Files.write(tempFile, fileContent);
            }

            Path bundlePath = resolveAttachmentBundlePath(options);
            LegacyDcImportOrchestrator.PrepareResult prepared = legacyDcImportOrchestrator.prepare(
                    jobId, tempFile, bundlePath, options, null);

            LegacyDcXmlParser.ParseResult parseResult = prepared.parseResult();
            log.info("Parsed XML format {} with {} entities (importable={})",
                    parseResult.getXmlFormat(), parseResult.getTotalEntities(),
                    prepared.importableEntities().size());

            if (prepared.blocked()) {
                migrationService.markJobFailed(jobId,
                        "Import blocked: " + prepared.validationReport().blockerCount() + " validation error(s)",
                        Map.of("riskScore", prepared.validationReport().riskScore()));
                sendJobFailed(jobId, userIdStr, "Validation blocked import");
                return CompletableFuture.failedFuture(new IllegalStateException("Validation blocked import"));
            }

            if (prepared.dryRun()) {
                Map<String, Object> dryMeta = Map.of(
                        "dryRun", true,
                        "riskScore", prepared.validationReport().riskScore(),
                        "blockers", prepared.validationReport().blockerCount(),
                        "warnings", prepared.validationReport().warningCount(),
                        "format", parseResult.getXmlFormat().name(),
                        "totalEntities", parseResult.getTotalEntities()
                );
                migrationService.markJobCompleted(jobId, dryMeta);
                sendJobCompleted(jobId, userIdStr, 0, 0);
                return CompletableFuture.completedFuture(migrationService.getImportResult(jobId));
            }

            List<LegacyDcXmlParser.ParsedEntity> importableEntities =
                    mergeAuxiliaryIntoIssues(prepared.importableEntities());
            LegacyDcIssueIdRegistry issueIdRegistry =
                    LegacyDcIssueIdRegistry.fromEntities(parseResult.getEntities());

            migrationService.setTotalEntities(jobId, importableEntities.size());
            sendProgressUpdate(jobId, userIdStr, 0, importableEntities.size(), 0, "PROCESSING", null);

            Map<String, Integer> processedByType = new HashMap<>();
            Map<String, String> issueKeyToTargetId = new ConcurrentHashMap<>();
            java.util.concurrent.atomic.AtomicLong attachmentBytesWritten = new java.util.concurrent.atomic.AtomicLong(0);
            java.util.concurrent.atomic.AtomicInteger incrementalSkipped = new java.util.concurrent.atomic.AtomicInteger(0);
            List<String[]> pendingIssueLinks = Collections.synchronizedList(new ArrayList<>());
            java.util.concurrent.atomic.AtomicInteger totalProcessed = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger totalFailed = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger commentCount = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger attachmentCount = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger attachmentChecksumChecked =
                    new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger attachmentChecksumMatched =
                    new java.util.concurrent.atomic.AtomicInteger(0);

            boolean resume = options != null && Boolean.TRUE.equals(options.get("resume"));
            int parallelWorkers = options != null && options.get("parallelWorkers") instanceof Number n
                    ? Math.max(1, Math.min(8, n.intValue())) : 1;

            List<String> processingOrder = Arrays.asList(dcProcessingOrderStr.split(","));

            ExecutorService workers = parallelWorkers > 1
                    ? Executors.newFixedThreadPool(parallelWorkers)
                    : null;

            boolean historyOnlyImport = options != null && Boolean.TRUE.equals(options.get("historyOnlyImport"));
            boolean historyReplayOnly = options != null && Boolean.TRUE.equals(options.get("historyReplayOnly"));

            for (String entityType : processingOrder) {
                List<LegacyDcXmlParser.ParsedEntity> typeEntities = importableEntities.stream()
                        .filter(e -> entityType.equals(e.getEntityType()))
                        .toList();

                updateStageProgress(jobId, entityType.toUpperCase(), 0, typeEntities.size());
                int processedSoFar = totalProcessed.get() + totalFailed.get();
                sendProgressUpdate(jobId, userIdStr, processedSoFar,
                        parseResult.getTotalEntities(), totalFailed.get(), "PROCESSING", entityType);

                boolean parallelSafe = workers != null
                        && Arrays.asList(parallelEntityTypesStr.split(",")).contains(entityType);

                if (parallelSafe && !typeEntities.isEmpty()) {
                    List<LegacyDcXmlParser.ParsedEntity> toProcess = resume
                            ? typeEntities.stream()
                            .filter(e -> !isEntityAlreadyCompleted(jobId, e.getEntityKey()))
                            .toList()
                            : typeEntities;
                    if (!toProcess.isEmpty()) {
                        processEntitiesParallel(toProcess, workers, jobId, issueKeyToTargetId,
                                pendingIssueLinks, userId, options, bundlePath, issueIdRegistry,
                                attachmentBytesWritten, incrementalSkipped,
                                totalProcessed, totalFailed, commentCount, attachmentCount,
                                attachmentChecksumChecked, attachmentChecksumMatched);
                    }
                    if (resume) {
                        long skipped = typeEntities.size() - toProcess.size();
                        totalProcessed.addAndGet((int) skipped);
                    }
                } else {
                    for (LegacyDcXmlParser.ParsedEntity entity : typeEntities) {
                        if (isJobPaused(jobId)) {
                            Thread.sleep(500);
                            continue;
                        }
                        if (resume && isEntityAlreadyCompleted(jobId, entity.getEntityKey())) {
                            totalProcessed.incrementAndGet();
                            continue;
                        }
                        EntityStatus status = EntityStatus.builder()
                                .jobId(jobId)
                                .entityType(entity.getEntityType())
                                .entityKey(entity.getEntityKey())
                                .status("PROCESSING")
                                .processingOrder(totalProcessed.get() + totalFailed.get())
                                .build();
                        entityStatusRepository.save(status);

                        try {
                            boolean ok = persistDcEntity(entity, jobId, issueKeyToTargetId, pendingIssueLinks,
                                    status, userId, options, bundlePath, issueIdRegistry,
                                    attachmentBytesWritten, attachmentCount, incrementalSkipped, historyOnlyImport,
                                    historyReplayOnly,
                                    attachmentChecksumChecked, attachmentChecksumMatched);
                            if (ok) {
                                status.markCompleted(status.getEntityId());
                                if ("Comment".equals(entity.getEntityType())) {
                                    commentCount.incrementAndGet();
                                }
                                totalProcessed.incrementAndGet();
                            } else {
                                status.markFailed("PERSIST_ERROR", "Failed to persist entity", null);
                                entityStatusRepository.save(status);
                                totalFailed.incrementAndGet();
                            }
                        } catch (Exception e) {
                            log.error("Error processing entity {}: {}", entity.getEntityKey(), e.getMessage());
                            status.markFailed("PROCESSING_ERROR", e.getMessage(), null);
                            entityStatusRepository.save(status);
                            recordFailure(jobId, entity.getEntityType(), entity.getEntityKey(),
                                    null, "PROCESSING_ERROR", e.getMessage(), null);
                            totalFailed.incrementAndGet();
                            sendErrorNotification(jobId, userIdStr, "PROCESSING_ERROR",
                                    e.getMessage(), entity.getEntityType(), entity.getEntityKey(), null);
                        }

                        int done = totalProcessed.get() + totalFailed.get();
                        if (done % progressUpdateBatchSize == 0) {
                            migrationService.updateJobProgress(jobId, totalProcessed.get(), totalFailed.get());
                            sendProgressUpdate(jobId, userIdStr, done,
                                    parseResult.getTotalEntities(), totalFailed.get(), "PROCESSING", entityType);
                        }
                    }
                }
                processedByType.put(entityType, typeEntities.size());
                updateStageProgress(jobId, entityType.toUpperCase(), typeEntities.size(), typeEntities.size());
            }

            if (workers != null) {
                workers.shutdown();
            }

            List<LegacyDcXmlParser.ParsedEntity> histories = importableEntities.stream()
                    .filter(e -> "History".equals(e.getEntityType()))
                    .toList();
            int historyReplayed = changeHistoryReplayer.replay(
                    jobId, histories, issueKeyToTargetId, issueIdRegistry,
                    options != null && Boolean.TRUE.equals(options.get("stubDownstream")));

            // Issue link pass (parent / epic) after all issues exist
            for (String[] link : pendingIssueLinks) {
                try {
                    if (link[2] != null) {
                        issueLinkPersisterHandler.persistParentChild(link[0], link[2], jobId);
                    }
                    if (link[1] != null) {
                        issueLinkPersisterHandler.persistEpicLink(link[0], link[1], jobId);
                    }
                } catch (Exception e) {
                    log.warn("DC link pass failed {}: {}", link[0], e.getMessage());
                }
            }

            sendProgressUpdate(jobId, userIdStr, totalProcessed.get() + totalFailed.get(),
                    parseResult.getTotalEntities(), totalFailed.get(), "COMPLETING", null);

            if (Boolean.TRUE.equals(options != null ? options.get("stubDownstream") : null)) {
                log.info("DC import job {} completed in stubDownstream mode (no external service calls)", jobId);
            }

            Map<String, Object> resultMetadata = new HashMap<>();
            resultMetadata.put("processedByType", processedByType);
            resultMetadata.put("totalProcessed", totalProcessed.get());
            resultMetadata.put("totalFailed", totalFailed.get());
            resultMetadata.put("commentCount", commentCount.get());
            resultMetadata.put("attachmentCount", attachmentCount.get());
            resultMetadata.put("attachmentBytesWritten", attachmentBytesWritten.get());
            resultMetadata.put("historyReplayed", historyReplayed);
            resultMetadata.put("historyOnlyImport", historyOnlyImport);
            resultMetadata.put("historyReplayOnly", historyReplayOnly);
            resultMetadata.put("incrementalSkipped", incrementalSkipped.get());
            resultMetadata.put("format", parseResult.getXmlFormat() != null ? parseResult.getXmlFormat().name() : null);
            resultMetadata.put("riskScore", prepared.validationReport().riskScore());
            resultMetadata.put("relationshipEdges", prepared.relationshipEdges());
            if (options != null && Boolean.TRUE.equals(options.get("stubDownstream"))) {
                resultMetadata.put("stubDownstream", true);
            }
            resultMetadata.put("referenceCatalogSize", referenceCatalog.size(jobId));
            int entitiesExpected = parseResult.getTotalEntities();
            int processed = totalProcessed.get() + totalFailed.get();
            boolean stubDownstream = options != null && Boolean.TRUE.equals(options.get("stubDownstream"));
            Map<String, Object> paritySummary = LegacyDcParitySummaryBuilder.build(
                    entitiesExpected,
                    processed,
                    totalFailed.get(),
                    historyReplayed,
                    incrementalSkipped.get(),
                    attachmentBytesWritten.get(),
                    attachmentCount.get(),
                    referenceCatalog.size(jobId),
                    parseResult.getXmlFormat() != null ? parseResult.getXmlFormat().name() : null,
                    prepared.validationReport().riskScore(),
                    processedByType,
                    historyOnlyImport,
                    stubDownstream);
            resultMetadata.put("paritySummary", paritySummary);
            resultMetadata.put("entitiesExpected", entitiesExpected);
            int checksumChecked = attachmentChecksumChecked.get();
            int checksumMatched = attachmentChecksumMatched.get();
            if (checksumChecked > 0) {
                double rate = (checksumMatched * 100.0) / checksumChecked;
                resultMetadata.put("attachmentChecksumMatchRate", Math.round(rate * 10) / 10.0);
                resultMetadata.put("attachmentChecksumChecked", checksumChecked);
                resultMetadata.put("attachmentChecksumMatched", checksumMatched);
            }
            MigrationJob completedJobForSla = migrationJobRepository.findById(jobId).orElse(null);
            long jobDurationMs = 0L;
            if (completedJobForSla != null && completedJobForSla.getStartedAt() != null
                    && completedJobForSla.getCompletedAt() != null) {
                jobDurationMs = java.time.Duration.between(
                        completedJobForSla.getStartedAt(), completedJobForSla.getCompletedAt()).toMillis();
            }
            int issueCountForSla = processedByType.getOrDefault("Issue", 0)
                    + processedByType.getOrDefault("SubTask", 0);
            Map<String, Object> slaProof = LegacyDcImportSlaProofBuilder.build(
                    issueCountForSla, jobDurationMs, totalFailed.get(), stubDownstream, "LIVE_IMPORT_JOB");
            resultMetadata.put("slaProof", slaProof);
            Map<String, Object> acSignoff = LegacyDcAcSignoffEvaluator.evaluate(
                    resultMetadata,
                    options != null ? options : Map.of(),
                    completedJobForSla != null && completedJobForSla.getJobStatus() != null
                            ? completedJobForSla.getJobStatus() : "COMPLETED",
                    entitiesExpected,
                    totalFailed.get());
            resultMetadata.put("acSignoff", acSignoff);
            migrationJobRepository.findById(jobId).ifPresent(j -> {
                if (j.getResultMetadata() != null && j.getResultMetadata().get("stages") != null) {
                    resultMetadata.put("stages", j.getResultMetadata().get("stages"));
                }
            });
            migrationService.markJobCompleted(jobId, resultMetadata);
            referenceCatalog.clear(jobId);
            schedulePostImportReindex(jobId);
            migrationAuditPersistenceService.log(jobId, "IMPORT_COMPLETED", "JOB", jobId.toString(), userId,
                    Map.of("processed", totalProcessed.get(), "failed", totalFailed.get(),
                            "comments", commentCount.get(), "attachments", attachmentCount.get()));
            sendJobCompleted(jobId, userIdStr, totalProcessed.get(), totalFailed.get());

            return CompletableFuture.completedFuture(migrationService.getImportResult(jobId));

        } catch (Exception e) {
            log.error("Legacy DC import job failed: {}", e.getMessage(), e);
            migrationService.markJobFailed(jobId, e.getMessage(), null);
            if (options != null && Boolean.TRUE.equals(options.get("rollbackOnFailure"))) {
                try {
                    migrationRollbackService.rollbackJob(jobId);
                } catch (Exception rb) {
                    log.warn("Auto-rollback after DC failure failed: {}", rb.getMessage());
                }
            }
            sendJobFailed(jobId, userIdStr, e.getMessage());
            return CompletableFuture.failedFuture(e);
        } finally {
            if (options != null && options.get("extractedBackupRoot") != null) {
                try {
                    legacyDcImportOrchestrator.cleanupExtractedRoot(
                            Path.of(options.get("extractedBackupRoot").toString()));
                } catch (Exception ignored) {
                    // ignore cleanup failures
                }
            }
            if (tempFile != null && (options == null || options.get("xmlPath") == null)) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // ignore
                }
            }
            com.avionics_systems.migration.security.MigrationRequestContext.clear();
        }
    }

    private void processEntitiesParallel(
            List<LegacyDcXmlParser.ParsedEntity> typeEntities,
            ExecutorService workers,
            UUID jobId,
            Map<String, String> issueKeyToTargetId,
            List<String[]> pendingIssueLinks,
            UUID userId,
            Map<String, Object> options,
            Path bundlePath,
            LegacyDcIssueIdRegistry issueIdRegistry,
            java.util.concurrent.atomic.AtomicLong attachmentBytesWritten,
            java.util.concurrent.atomic.AtomicInteger incrementalSkipped,
            java.util.concurrent.atomic.AtomicInteger totalProcessed,
            java.util.concurrent.atomic.AtomicInteger totalFailed,
            java.util.concurrent.atomic.AtomicInteger commentCount,
            java.util.concurrent.atomic.AtomicInteger attachmentCount,
            java.util.concurrent.atomic.AtomicInteger attachmentChecksumChecked,
            java.util.concurrent.atomic.AtomicInteger attachmentChecksumMatched) throws Exception {

        List<java.util.concurrent.Future<Boolean>> futures = new ArrayList<>();
        boolean historyOnlyImport = options != null && Boolean.TRUE.equals(options.get("historyOnlyImport"));
        boolean historyReplayOnly = options != null && Boolean.TRUE.equals(options.get("historyReplayOnly"));
        for (LegacyDcXmlParser.ParsedEntity entity : typeEntities) {
            futures.add(workers.submit(() -> {
                EntityStatus status = EntityStatus.builder()
                        .jobId(jobId)
                        .entityType(entity.getEntityType())
                        .entityKey(entity.getEntityKey())
                        .status("PROCESSING")
                        .build();
                entityStatusRepository.save(status);
                boolean ok = persistDcEntity(entity, jobId, issueKeyToTargetId, pendingIssueLinks,
                        status, userId, options, bundlePath, issueIdRegistry,
                        attachmentBytesWritten, attachmentCount, incrementalSkipped, historyOnlyImport,
                        historyReplayOnly,
                        attachmentChecksumChecked, attachmentChecksumMatched);
                if (ok) {
                    status.markCompleted(status.getEntityId());
                    if ("Comment".equals(entity.getEntityType())) {
                        commentCount.incrementAndGet();
                    }
                    totalProcessed.incrementAndGet();
                } else {
                    status.markFailed("PERSIST_ERROR", "Failed to persist entity", null);
                    entityStatusRepository.save(status);
                    totalFailed.incrementAndGet();
                }
                return ok;
            }));
        }
        for (java.util.concurrent.Future<Boolean> f : futures) {
            f.get();
        }
    }

    private Path resolveAttachmentBundlePath(Map<String, Object> options) {
        if (options == null || options.get("attachmentBundlePath") == null) {
            return null;
        }
        return Path.of(options.get("attachmentBundlePath").toString());
    }

    private boolean isEntityAlreadyCompleted(UUID jobId, String entityKey) {
        return entityStatusRepository.findFirstByJobIdAndEntityKey(jobId, entityKey)
                .map(s -> "COMPLETED".equals(s.getStatus()) || "SUCCESS".equals(s.getStatus()))
                .orElse(false);
    }

    private void updateAttachmentProgress(UUID jobId, long bytesWritten, int attachmentCount) {
        migrationJobRepository.findById(jobId).ifPresent(job -> {
            Map<String, Object> meta = job.getResultMetadata() != null
                    ? new HashMap<>(job.getResultMetadata())
                    : new HashMap<>();
            meta.put("attachmentBytesWritten", bytesWritten);
            meta.put("attachmentsCompleted", attachmentCount);
            job.setResultMetadata(meta);
            migrationJobRepository.save(job);
        });
    }

    private static String firstField(Map<String, String> fields, String... keys) {
        for (String key : keys) {
            if (fields.containsKey(key) && fields.get(key) != null && !fields.get(key).isBlank()) {
                return fields.get(key);
            }
        }
        return null;
    }

    private static List<LegacyDcXmlParser.ParsedEntity> mergeAuxiliaryIntoIssues(
            List<LegacyDcXmlParser.ParsedEntity> entities) {
        Map<String, LegacyDcXmlParser.ParsedEntity> issuesByKey = new HashMap<>();
        List<LegacyDcXmlParser.ParsedEntity> result = new ArrayList<>();
        for (LegacyDcXmlParser.ParsedEntity e : entities) {
            if ("Issue".equals(e.getEntityType()) || "SubTask".equals(e.getEntityType())) {
                issuesByKey.put(e.getEntityKey(), e);
            }
        }
        for (LegacyDcXmlParser.ParsedEntity e : entities) {
            if ("CustomField".equals(e.getEntityType())) {
                applyCustomField(issuesByKey, entities, e);
            } else if ("PluginEntity".equals(e.getEntityType())) {
                applyPluginAsCustomField(issuesByKey, entities, e);
            } else if ("Label".equals(e.getEntityType())) {
                applyLabel(issuesByKey, e);
            } else if (!"CustomField".equals(e.getEntityType())
                    && !"PluginEntity".equals(e.getEntityType())
                    && !"Label".equals(e.getEntityType())) {
                result.add(e);
            }
        }
        return result;
    }

    private static void applyCustomField(Map<String, LegacyDcXmlParser.ParsedEntity> issuesByKey,
                                         List<LegacyDcXmlParser.ParsedEntity> entities,
                                         LegacyDcXmlParser.ParsedEntity cfEntity) {
        Map<String, String> f = cfEntity.getFields();
        if (f == null) {
            return;
        }
        String issueKey = f.get("issueKey");
        if (issueKey == null) {
            return;
        }
        LegacyDcXmlParser.ParsedEntity issue = issuesByKey.get(issueKey);
        if (issue == null) {
            for (LegacyDcXmlParser.ParsedEntity candidate : entities) {
                if ("Issue".equals(candidate.getEntityType()) && issueKey.equals(candidate.getEntityKey())) {
                    issue = candidate;
                    issuesByKey.put(issueKey, issue);
                    break;
                }
            }
        }
        if (issue != null && issue.getFields() != null) {
            String cfId = f.get("customFieldId");
            String value = f.get("value");
            if (cfId != null && value != null) {
                issue.getFields().put(cfId, value);
            }
        }
    }

    private static void applyPluginAsCustomField(Map<String, LegacyDcXmlParser.ParsedEntity> issuesByKey,
                                                List<LegacyDcXmlParser.ParsedEntity> entities,
                                                LegacyDcXmlParser.ParsedEntity pluginEntity) {
        Map<String, String> f = pluginEntity.getFields();
        if (f == null) {
            return;
        }
        String issueKey = f.get("issueKey");
        if (issueKey == null) {
            return;
        }
        LegacyDcXmlParser.ParsedEntity cf = new LegacyDcXmlParser.ParsedEntity();
        cf.setEntityType("CustomField");
        Map<String, String> cfFields = new HashMap<>(f);
        String fieldId = f.get("field");
        if (fieldId != null && !fieldId.isBlank()) {
            cfFields.put("customFieldId", fieldId.startsWith("customfield_") ? fieldId : "customfield_plugin");
        } else {
            cfFields.put("customFieldId", "customfield_" + f.getOrDefault("pluginType", "plugin"));
        }
        cfFields.put("value", f.getOrDefault("value", ""));
        cf.setFields(cfFields);
        cf.setEntityKey(issueKey + ":plugin:" + pluginEntity.getEntityKey());
        applyCustomField(issuesByKey, entities, cf);
    }

    private static void applyLabel(Map<String, LegacyDcXmlParser.ParsedEntity> issuesByKey,
                                   LegacyDcXmlParser.ParsedEntity labelEntity) {
        Map<String, String> f = labelEntity.getFields();
        if (f == null) {
            return;
        }
        String issueKey = f.get("issueKey");
        String label = f.get("label");
        if (issueKey == null || label == null) {
            return;
        }
        LegacyDcXmlParser.ParsedEntity issue = issuesByKey.get(issueKey);
        if (issue != null && issue.getFields() != null) {
            String existing = issue.getFields().get("labels");
            issue.getFields().put("labels",
                    existing == null || existing.isBlank() ? label : existing + "," + label);
        }
    }

    private static void recordAttachmentChecksum(
            Map<String, Object> metadata,
            String actualChecksum,
            java.util.concurrent.atomic.AtomicInteger checked,
            java.util.concurrent.atomic.AtomicInteger matched) {
        if (checked == null || actualChecksum == null || actualChecksum.isBlank()) {
            return;
        }
        Object expected = metadata != null ? metadata.get("expectedChecksum") : null;
        if (expected == null || String.valueOf(expected).isBlank()) {
            return;
        }
        checked.incrementAndGet();
        if (actualChecksum.equalsIgnoreCase(String.valueOf(expected))) {
            matched.incrementAndGet();
        }
    }

    private boolean persistDcEntity(
            LegacyDcXmlParser.ParsedEntity entity,
            UUID jobId,
            Map<String, String> issueKeyToTargetId,
            List<String[]> pendingIssueLinks,
            EntityStatus status,
            UUID userId,
            Map<String, Object> options,
            Path attachmentBundlePath,
            LegacyDcIssueIdRegistry issueIdRegistry,
            java.util.concurrent.atomic.AtomicLong attachmentBytesWritten,
            java.util.concurrent.atomic.AtomicInteger attachmentCount,
            java.util.concurrent.atomic.AtomicInteger incrementalSkipped,
            boolean historyOnlyImport,
            boolean historyReplayOnly,
            java.util.concurrent.atomic.AtomicInteger attachmentChecksumChecked,
            java.util.concurrent.atomic.AtomicInteger attachmentChecksumMatched) {

        Map<String, String> fields = entity.getFields() != null ? entity.getFields() : Map.of();
        String type = entity.getEntityType();
        boolean stub = options != null && Boolean.TRUE.equals(options.get("stubDownstream"));

        if (stub) {
            return persistDcEntityStub(entity, jobId, issueKeyToTargetId, status, fields, type);
        }

        return switch (type) {
            case "Issue", "SubTask" -> {
                if (historyReplayOnly) {
                    var prior = incrementalMigrationService.priorSuccess(jobId, entity.getEntityKey());
                    if (prior.isPresent() && prior.get().getTargetIssueId() != null) {
                        issueKeyToTargetId.put(
                                entity.getEntityKey(), prior.get().getTargetIssueId().toString());
                        status.markSkipped("History replay only — issue not created");
                        entityStatusRepository.save(status);
                        yield true;
                    }
                    status.markSkipped("History replay only — no prior issue mapping for " + entity.getEntityKey());
                    entityStatusRepository.save(status);
                    yield false;
                }
                boolean incremental = options != null && Boolean.TRUE.equals(options.get("incrementalDelta"));
                if (incremental && incrementalMigrationService.shouldSkipIssue(jobId, entity.getEntityKey())) {
                    incrementalMigrationService.priorSuccess(jobId, entity.getEntityKey()).ifPresent(prior -> {
                        if (prior.getTargetIssueId() != null) {
                            issueKeyToTargetId.put(entity.getEntityKey(), prior.getTargetIssueId().toString());
                        }
                    });
                    status.markSkipped("Already imported (incremental delta)");
                    entityStatusRepository.save(status);
                    if (incrementalSkipped != null) {
                        incrementalSkipped.incrementAndGet();
                    }
                    yield true;
                }
                Map<String, Object> issueData = LegacyDcEntityMapper.toIssueData(fields, entity.getEntityKey());
                var issueResult = issuePersisterHandler.persistIssue(issueData, jobId);
                if (issueResult.isSuccess() && issueResult.getIssueId() != null) {
                    status.setTargetId(issueResult.getIssueId().toString());
                    status.setEntityId(issueResult.getIssueId());
                    issueKeyToTargetId.put(entity.getEntityKey(), issueResult.getIssueId().toString());
                    migrationIssueResultService.recordSuccess(
                            jobId, entity.getEntityKey(), issueResult, null);
                    String sourceStatus = (String) issueData.get("status");
                    if (!historyOnlyImport && sourceStatus != null && !sourceStatus.isBlank()) {
                        try {
                            var issue = issueServiceClient.getIssue(issueResult.getIssueId().toString());
                            if (issue != null) {
                                migrationWorkflowStatusApplier.applyImportedStatus(jobId, issue, sourceStatus);
                            }
                        } catch (Exception ex) {
                            log.debug("Workflow status apply skipped for {}: {}", entity.getEntityKey(), ex.getMessage());
                        }
                    }
                    String parent = (String) issueData.get("parentIssueKey");
                    String epic = (String) issueData.get("epicLink");
                    if (parent != null || epic != null) {
                        pendingIssueLinks.add(new String[]{entity.getEntityKey(), epic, parent});
                    }
                } else {
                    migrationIssueResultService.recordFailure(
                            jobId, entity.getEntityKey(), issueResult.getErrorMessage(), null);
                }
                entityStatusRepository.save(status);
                yield issueResult.isSuccess();
            }
            case "Comment" -> {
                Map<String, Object> commentData = LegacyDcEntityMapper.toCommentData(
                        fields, entity.getEntityKey(), issueKeyToTargetId, issueIdRegistry);
                var result = commentPersisterHandler.persistComment(commentData, jobId);
                if (result.isSuccess() && result.getCommentId() != null) {
                    status.setTargetId(result.getCommentId().toString());
                    status.setEntityId(result.getCommentId());
                }
                entityStatusRepository.save(status);
                yield result.isSuccess();
            }
            case "Attachment" -> {
                LegacyDcEntityMapper.AttachmentPayload payload = LegacyDcEntityMapper.toAttachmentPayload(
                        fields, entity.getEntityKey(), issueKeyToTargetId, issueIdRegistry);
                byte[] content = payload.content();
                if (content.length == 0 && attachmentBundlePath != null) {
                    LegacyDcAttachmentBundleResolver.ResolvedAttachment resolved = attachmentBundleResolver.resolve(
                            attachmentBundlePath,
                            fields.get("sourceAttachmentId"),
                            firstField(fields, "filename", "name"));
                    if (resolved.hasContent()) {
                        payload = new LegacyDcEntityMapper.AttachmentPayload(payload.metadata(), resolved.content());
                        if (resolved.mimeType() != null) {
                            payload.metadata().put("mimeType", resolved.mimeType());
                        }
                        if (resolved.checksum() != null) {
                            payload.metadata().put("expectedChecksum", resolved.checksum());
                        }
                        content = resolved.content();
                    }
                }
                if (content.length == 0) {
                    status.markSkipped("No attachment content in DC export (metadata recorded)");
                    entityStatusRepository.save(status);
                    yield true;
                }
                var result = attachmentPersisterHandler.persistAttachment(
                        payload.metadata(), payload.content(), jobId);
                if (result.isSuccess()) {
                    if (result.getAttachmentId() != null) {
                        status.setTargetId(result.getAttachmentId().toString());
                        status.setEntityId(result.getAttachmentId());
                    }
                    recordAttachmentChecksum(
                            payload.metadata(), result.getChecksum(),
                            attachmentChecksumChecked, attachmentChecksumMatched);
                    if (attachmentBytesWritten != null && content.length > 0) {
                        attachmentBytesWritten.addAndGet(content.length);
                        if (attachmentCount != null) {
                            int attNum = attachmentCount.incrementAndGet();
                            updateAttachmentProgress(jobId, attachmentBytesWritten.get(), attNum);
                        }
                    }
                    migrationAttachmentResultService.recordSuccess(
                            jobId,
                            (String) payload.metadata().get("issueKey"),
                            result.getAttachmentId(),
                            result.getFileName(),
                            result.getChecksum());
                } else {
                    migrationAttachmentResultService.recordFailure(
                            jobId,
                            (String) payload.metadata().get("issueKey"),
                            result.getFileName(),
                            result.getErrorMessage());
                }
                entityStatusRepository.save(status);
                yield result.isSuccess();
            }
            case "Worklog" -> {
                Map<String, Object> worklogData = LegacyDcEntityMapper.toWorklogData(
                        fields, entity.getEntityKey(), issueKeyToTargetId, issueIdRegistry);
                var result = worklogPersisterHandler.persistWorklog(worklogData, jobId);
                if (result.isSuccess() && result.getWorklogId() != null) {
                    status.setTargetId(result.getWorklogId().toString());
                    status.setEntityId(result.getWorklogId());
                }
                entityStatusRepository.save(status);
                yield result.isSuccess();
            }
            case "Project" -> {
                Map<String, Object> projectData = new HashMap<>(fields);
                String projectKey = fields.getOrDefault("key", entity.getEntityKey());
                projectData.put("key", projectKey);
                projectData.put("projectKey", projectKey);
                projectData.put("name", fields.getOrDefault("name", projectKey));
                var result = projectPersisterHandler.persistProject(projectData, jobId);
                if (result.isSuccess() && result.getProjectId() != null) {
                    status.setTargetId(result.getProjectId().toString());
                }
                entityStatusRepository.save(status);
                yield result.isSuccess();
            }
            case "IssueLink" -> {
                Map<String, Object> linkData = LegacyDcEntityMapper.toIssueLinkData(fields, entity.getEntityKey());
                var linkResult = issueLinkPersisterHandler.persistIssueLink(linkData, jobId);
                entityStatusRepository.save(status);
                yield linkResult.isSuccess();
            }
            case "History" -> {
                status.markSkipped("History replayed in post-pass");
                entityStatusRepository.save(status);
                yield true;
            }
            case "Label" -> {
                String issueKey = fields.get("issueKey");
                if (issueKey == null && issueIdRegistry != null) {
                    issueKey = issueIdRegistry.resolveIssueKey(fields.get("issueId"));
                }
                String label = fields.get("label");
                if (issueKey != null && label != null) {
                    labelPersisterHandler.persistLabelsForIssue(issueKey, List.of(label), jobId);
                }
                status.markCompleted(null);
                entityStatusRepository.save(status);
                yield true;
            }
            case "Component" -> {
                var result = componentPersisterHandler.persistComponent(
                        LegacyDcEntityMapper.toComponentData(fields, entity.getEntityKey()), jobId);
                if (result.isSuccess() && result.getComponentId() != null) {
                    status.setTargetId(result.getComponentId().toString());
                }
                entityStatusRepository.save(status);
                yield result.isSuccess();
            }
            case "Version" -> {
                var result = versionPersisterHandler.persistVersion(
                        LegacyDcEntityMapper.toVersionData(fields, entity.getEntityKey()), jobId);
                if (result.isSuccess() && result.getVersionId() != null) {
                    status.setTargetId(result.getVersionId().toString());
                }
                entityStatusRepository.save(status);
                yield result.isSuccess();
            }
            case "Watcher" -> {
                String issueKey = fields.get("issueKey");
                if (issueKey == null && issueIdRegistry != null) {
                    issueKey = issueIdRegistry.resolveIssueKey(fields.get("issueId"));
                }
                String targetId = issueKey != null ? issueKeyToTargetId.get(issueKey) : null;
                if (targetId != null) {
                    issueServiceClient.watchIssue(targetId);
                }
                status.markCompleted(null);
                entityStatusRepository.save(status);
                yield true;
            }
            case "Vote" -> {
                status.markCompleted(null);
                entityStatusRepository.save(status);
                yield true;
            }
            case "PluginEntity" -> {
                referenceCatalog.record(jobId, type, entity.getEntityKey(), fields);
                status.markCompleted(null);
                entityStatusRepository.save(status);
                yield true;
            }
            case "IssueType", "Status", "Priority", "Resolution", "CustomField", "Group", "Workflow" -> {
                referenceCatalog.record(jobId, type, entity.getEntityKey(), fields);
                status.markCompleted(null);
                entityStatusRepository.save(status);
                yield true;
            }
            case "User" -> {
                String username = fields.getOrDefault("lowerUserName", fields.get("userKey"));
                if (username != null) {
                    userPersisterHandler.persistUserMapping(
                            jobId, username, "DC_USER", null, username, "IMPORT");
                }
                status.markCompleted(null);
                entityStatusRepository.save(status);
                yield true;
            }
            default -> {
                referenceCatalog.record(jobId, type, entity.getEntityKey(), fields);
                status.markCompleted(null);
                entityStatusRepository.save(status);
                yield true;
            }
        };
    }

    /**
     * Local verification path: parses Comment/Attachment (incl. base64 + checksum) without calling issue/attachment services.
     */
    private boolean persistDcEntityStub(
            LegacyDcXmlParser.ParsedEntity entity,
            UUID jobId,
            Map<String, String> issueKeyToTargetId,
            EntityStatus status,
            Map<String, String> fields,
            String type) {

        return switch (type) {
            case "Project" -> {
                String projectKey = fields.getOrDefault("key", defaultProjectKey);
                UUID targetId = UUID.randomUUID();
                projectMappingRepository.save(ProjectMapping.builder()
                        .jobId(jobId)
                        .sourceKey(projectKey)
                        .targetKey(projectKey)
                        .targetId(targetId)
                        .issueKeySequence(0)
                        .build());
                status.setTargetId(targetId.toString());
                status.markCompleted(null);
                entityStatusRepository.save(status);
                yield true;
            }
            case "Issue", "SubTask" -> {
                UUID fakeId = UUID.randomUUID();
                issueKeyToTargetId.put(entity.getEntityKey(), fakeId.toString());
                status.setTargetId(fakeId.toString());
                status.setEntityId(fakeId);
                migrationIssueResultService.recordSuccess(jobId, entity.getEntityKey(),
                        stubIssueResult(fakeId, entity.getEntityKey()), null);
                status.markCompleted(fakeId);
                entityStatusRepository.save(status);
                yield true;
            }
            case "Comment" -> {
                String body = fields.getOrDefault("body", fields.get("comment"));
                if (body == null || body.isBlank()) {
                    status.markFailed("VALIDATION_ERROR", "Comment body is required", null);
                    entityStatusRepository.save(status);
                    yield false;
                }
                UUID commentId = UUID.randomUUID();
                status.setTargetId(commentId.toString());
                status.setEntityId(commentId);
                status.markCompleted(commentId);
                entityStatusRepository.save(status);
                yield true;
            }
            case "Attachment" -> {
                LegacyDcEntityMapper.AttachmentPayload payload = LegacyDcEntityMapper.toAttachmentPayload(
                        fields, entity.getEntityKey(), issueKeyToTargetId);
                if (payload.content().length == 0) {
                    status.markSkipped("No attachment content");
                    entityStatusRepository.save(status);
                    yield true;
                }
                String checksum = ChunkedAttachmentUploadService.sha256Hex(payload.content());
                String fileName = (String) payload.metadata().getOrDefault("fileName", "attachment.bin");
                UUID attId = UUID.randomUUID();
                migrationAttachmentResultService.recordSuccess(
                        jobId,
                        (String) payload.metadata().get("issueKey"),
                        attId,
                        fileName,
                        checksum);
                status.setTargetId(attId.toString());
                status.setEntityId(attId);
                status.markCompleted(attId);
                entityStatusRepository.save(status);
                yield true;
            }
            default -> {
                status.markSkipped("Stub skip: " + type);
                entityStatusRepository.save(status);
                yield true;
            }
        };
    }

    private IssuePersisterHandler.IssuePersisterResult stubIssueResult(UUID id, String key) {
        IssuePersisterHandler.IssuePersisterResult r = new IssuePersisterHandler.IssuePersisterResult();
        r.setSuccess(true);
        r.setIssueId(id);
        r.setIssueKey(key);
        return r;
    }

    @Async("migrationTaskExecutor")
    public CompletableFuture<ImportResultResponse> processJiraDcApiImport(
            UUID jobId,
            JiraDcConnectionConfig config,
            Map<String, Object> options,
            UUID userId) {

        log.info("Starting Jira DC API import job: {} from {}", jobId, config.getBaseUrl());
        String userIdStr = userId != null ? userId.toString() : "system";

        try {
            com.avionics_systems.migration.security.MigrationRequestContext.setUserId(userId);
            migrationService.markJobStarted(jobId);
            migrationAuditPersistenceService.log(jobId, "IMPORT_STARTED", "JOB", jobId.toString(), userId,
                    Map.of("source", "JIRA_DC_API", "jiraBaseUrl", config.getBaseUrl()));
            migrationEventPublisher.enqueue(jobId, "IMPORT_STARTED", Map.of("source", "JIRA_DC_API"));
            sendProgressUpdate(jobId, userIdStr, 0, 0, 0, "CONNECTING", null);

            String targetProjectIdStr = options != null ? (String) options.get("targetProjectId") : null;

            @SuppressWarnings("unchecked")
            Map<String, String> fieldMappings = options != null && options.get("fieldMappings") instanceof Map
                    ? (Map<String, String>) options.get("fieldMappings") : null;
            if (fieldMappings != null && !fieldMappings.isEmpty()) {
                com.avionics_systems.migration.jiradc.JiraDcEntityMapper.registerFieldMappings(fieldMappings);
            }

            JiraDcApiImportOrchestrator.ImportResult result =
                    jiraDcApiImportOrchestrator.executeImport(jobId, config, userId, targetProjectIdStr);

            Map<String, Object> completionMeta = new HashMap<>(result.metadata());
            completionMeta.put("processedCount", result.processedCount());
            completionMeta.put("failedCount", result.failedCount());
            completionMeta.put("commentCount", result.commentCount());
            completionMeta.put("attachmentCount", result.attachmentCount());

            migrationService.markJobCompleted(jobId, completionMeta);
            migrationAuditPersistenceService.log(jobId, "IMPORT_COMPLETED", "JOB", jobId.toString(), userId,
                    completionMeta);
            migrationEventPublisher.enqueue(jobId, "IMPORT_COMPLETED", completionMeta);
            sendJobCompleted(jobId, userIdStr, result.processedCount(), result.failedCount());

            migrationJobReindexService.triggerReindex(jobId, List.of("ISSUE", "COMMENT"));

            log.info("Jira DC API import job {} completed: {} imported, {} failed, {} comments, {} attachments",
                    jobId, result.processedCount(), result.failedCount(),
                    result.commentCount(), result.attachmentCount());

            return CompletableFuture.completedFuture(migrationService.getImportResult(jobId));

        } catch (Exception e) {
            log.error("Jira DC API import job {} failed: {}", jobId, e.getMessage(), e);
            migrationService.markJobFailed(jobId, "Jira DC API import failed: " + e.getMessage(),
                    Map.of("error", e.getClass().getSimpleName()));
            migrationAuditPersistenceService.log(jobId, "IMPORT_FAILED", "JOB", jobId.toString(), userId,
                    Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown"));
            sendJobFailed(jobId, userIdStr, e.getMessage());
            return CompletableFuture.failedFuture(e);
        } finally {
            com.avionics_systems.migration.security.MigrationRequestContext.clear();
        }
    }

    @Async("migrationTaskExecutor")
    public CompletableFuture<ImportResultResponse> processProjectImport(
            UUID jobId,
            UUID sourceProjectId,
            UUID targetProjectId,
            Map<String, Object> options,
            UUID userId) {

        log.info("Starting project import: {} -> {}", sourceProjectId, targetProjectId);
        String userIdStr = userId != null ? userId.toString() : "system";

        try {
            migrationService.markJobStarted(jobId);

            // Track entities to import
            List<String> entityTypes = Arrays.asList(projectEntityTypesStr.split(","));

            int totalEntities = entityTypes.size();
            migrationService.setTotalEntities(jobId, totalEntities);

            sendProgressUpdate(jobId, userIdStr, 0, totalEntities, 0, "PROCESSING", null);

            int processed = 0;
            int failed = 0;

            for (String entityType : entityTypes) {
                try {
                    EntityStatus status = EntityStatus.builder()
                            .jobId(jobId)
                            .entityType(entityType)
                            .status("PROCESSING")
                            .processingOrder(processed + failed)
                            .build();
                    entityStatusRepository.save(status);

                    // Import entity type from source
                    boolean success = importEntityType(jobId, sourceProjectId, targetProjectId, entityType);

                    if (success) {
                        status.markCompleted(null);
                    } else {
                        status.markFailed("IMPORT_FAILED", "Import failed", null);
                        failed++;
                    }
                    entityStatusRepository.save(status);

                    processed++;
                    migrationService.updateJobProgress(jobId, processed, failed);

                    sendProgressUpdate(jobId, userIdStr, processed + failed,
                            totalEntities, failed, "PROCESSING", entityType);

                } catch (Exception e) {
                    log.error("Error importing {}: {}", entityType, e.getMessage());
                    recordFailure(jobId, entityType, null, null, "IMPORT_ERROR", e.getMessage(), null);
                    failed++;
                    migrationService.updateJobProgress(jobId, processed, failed);

                    sendErrorNotification(jobId, userIdStr, "IMPORT_ERROR",
                            e.getMessage(), entityType, null, null);
                }
            }

            Map<String, Object> resultMetadata = Map.of(
                    "projectImport", true,
                    "sourceProject", sourceProjectId.toString(),
                    "targetProject", targetProjectId.toString()
            );
            migrationService.markJobCompleted(jobId, resultMetadata);
            schedulePostImportReindex(jobId);

            sendJobCompleted(jobId, userIdStr, processed, failed);

            return CompletableFuture.completedFuture(migrationService.getImportResult(jobId));

        } catch (Exception e) {
            log.error("Project import failed: {}", e.getMessage(), e);
            migrationService.markJobFailed(jobId, e.getMessage(), null);

            sendJobFailed(jobId, userIdStr, e.getMessage());

            return CompletableFuture.failedFuture(e);
        }
    }

    private void schedulePostImportReindex(UUID jobId) {
        try {
            migrationEventPublisher.enqueue(jobId, "IMPORT_COMPLETED", Map.of("reindex", true));
            migrationJobReindexService.triggerReindex(jobId, Arrays.asList(reindexEntityTypesStr.split(",")));
        } catch (Exception e) {
            log.warn("Post-import reindex scheduling failed for {}: {}", jobId, e.getMessage());
        }
    }

    private byte[] resolveCsvAttachmentContent(Map<String, Object> attData) throws java.io.IOException {
        Object path = attData.get("attachmentPath");
        if (path != null && !path.toString().isBlank()) {
            return csvAttachmentResolver.resolveContent(path.toString());
        }
        Object url = attData.get("attachmentUrl");
        if (url != null && !url.toString().isBlank()) {
            return csvAttachmentResolver.resolveContent(url.toString());
        }
        return new byte[0];
    }

    @SuppressWarnings("unchecked")
    private int processCsvIssueColumnAttachments(
            List<Map<String, Object>> issueRows, UUID jobId, Map<String, Object> jobOptions) {
        String attachmentColumn = stringOption(jobOptions, "attachmentColumn", null);
        if (attachmentColumn == null || attachmentColumn.isBlank()) {
            attachmentColumn = "attachments";
        }
        int ok = 0;
        for (Map<String, Object> issueData : issueRows) {
            Object pending = issueData.get("_pendingAttachmentRefs");
            if (!(pending instanceof List<?> list) || list.isEmpty()) {
                continue;
            }
            String sourceKey = (String) issueData.get("issueKey");
            if (sourceKey == null) {
                continue;
            }
            var statusOpt = entityStatusRepository.findByJobIdAndEntityTypeAndSourceIdentifier(
                    jobId, "ISSUE", sourceKey);
            if (statusOpt.isEmpty() || statusOpt.get().getTargetId() == null) {
                continue;
            }
            String targetIssueId = statusOpt.get().getTargetId();
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> refMap)) {
                    continue;
                }
                Object refObj = refMap.get("reference");
                if (refObj == null) {
                    continue;
                }
                String reference = refObj.toString();
                Object fnObj = refMap.get("fileName");
                String fileName = fnObj != null ? fnObj.toString() : "attachment";
                try {
                    byte[] content = csvAttachmentResolver.resolveContent(reference);
                    if (content.length == 0) {
                        continue;
                    }
                    String scan = virusScanService.scanBytes(content, fileName);
                    if ("INFECTED".equals(scan)) {
                        continue;
                    }
                    Map<String, Object> attData = new HashMap<>();
                    attData.put("issueId", targetIssueId);
                    attData.put("fileName", fileName);
                    attData.put("attachmentPath", reference);
                    var result = attachmentPersisterHandler.persistAttachment(attData, content, jobId);
                    if (result != null && result.isSuccess()) {
                        ok++;
                    }
                } catch (Exception e) {
                    log.debug("CSV column attachment failed for {}: {}", sourceKey, e.getMessage());
                }
            }
        }
        if (ok > 0) {
            migrationJobLogService.appendLog(jobId, "INFO",
                    "CSV Attachments column: " + ok + " file(s) imported");
        }
        return ok;
    }

    private void validateCsvSingleTargetProject(
            List<Map<String, String>> rows, UUID targetProjectId, UUID jobId) {
        Set<String> distinct = new HashSet<>();
        for (Map<String, String> row : rows) {
            String pk = row.getOrDefault("project_key", row.get("project"));
            if (pk != null && !pk.isBlank()) {
                distinct.add(pk.trim().toUpperCase(Locale.ROOT));
            }
        }
        if (distinct.size() > 1) {
            migrationJobLogService.appendLog(jobId, "WARN",
                    "CSV contains multiple project keys " + distinct
                            + " — all rows map to single target project "
                            + (targetProjectId != null ? targetProjectId : "(unset)")
                            + ". For true multi-project import use Legacy DC XML + project import.");
        }
    }

    private String stringOption(Map<String, Object> options, String key, String defaultValue) {
        if (options == null) {
            return defaultValue;
        }
        Object v = options.get(key);
        return v != null ? v.toString() : defaultValue;
    }

    private boolean isJobPaused(UUID jobId) {
        return migrationJobControlService.isPaused(jobId);
    }

    private boolean importEntityType(UUID jobId, UUID sourceProjectId, UUID targetProjectId, String entityType) {
        ProjectImportOrchestrator.ImportEntityResult result =
                projectImportOrchestrator.importEntityType(jobId, sourceProjectId, targetProjectId, entityType);
        if (result.skipped()) {
            migrationJobLogService.appendLog(jobId, "INFO", result.message());
            return true;
        }
        return result.success();
    }

    private Map<String, String> convertRowToMap(String[] row, String[] headers) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < headers.length && i < row.length; i++) {
            map.put(headers[i].trim().toLowerCase().replace(" ", "_"), row[i]);
        }
        return map;
    }

    private void deriveProjectKeyForRows(List<Map<String, String>> rows, UUID targetProjectId) {
        String resolvedProjectKey = null;
        for (Map<String, String> row : rows) {
            String pk = row.get("project_key");
            if (pk == null || pk.isBlank()) pk = row.get("project key");
            if (pk == null || pk.isBlank()) pk = row.get("projectkey");
            if (pk != null && !pk.isBlank()) {
                continue;
            }
            String issueKey = row.get("issue_key");
            if (issueKey == null || issueKey.isBlank()) issueKey = row.get("issue key");
            if (issueKey == null || issueKey.isBlank()) issueKey = row.get("issuekey");
            if (issueKey != null && issueKey.contains("-")) {
                row.put("project_key", issueKey.substring(0, issueKey.lastIndexOf('-')));
                continue;
            }
            if (targetProjectId != null && resolvedProjectKey == null) {
                try {
                    var project = projectServiceClient.getProject(targetProjectId.toString());
                    if (project != null && project.getKey() != null && !project.getKey().isBlank()) {
                        resolvedProjectKey = project.getKey();
                    }
                } catch (Exception e) {
                    log.debug("Could not resolve project key for targetProjectId {}: {}", targetProjectId, e.getMessage());
                    resolvedProjectKey = "";
                }
            }
            if (resolvedProjectKey != null && !resolvedProjectKey.isBlank()) {
                row.put("project_key", resolvedProjectKey);
            }
        }
    }

    private void recordFailure(UUID jobId, String entityType, String entityKey,
                              Integer row, String errorCode, String errorMessage, String errorField) {
        EntityStatus status = EntityStatus.builder()
                .jobId(jobId)
                .entityType(entityType)
                .entityKey(entityKey)
                .status("FAILED")
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .errorRow(row)
                .errorField(errorField)
                .build();
        entityStatusRepository.save(status);
    }

    // ========== WebSocket Event Methods ==========

    private void sendProgressUpdate(UUID jobId, String userId, int processed, int total,
                                    int failed, String stage, String entityType) {
        int progressPercent = total > 0 ? (int) ((processed * 100.0) / total) : 0;

        JobProgressUpdate update = JobProgressUpdate.builder()
                .jobId(jobId.toString())
                .progressPercentage(progressPercent)
                .processedEntities(processed)
                .totalEntities(total)
                .failedEntities(failed)
                .currentStage(stage)
                .currentEntityType(entityType)
                .timestamp(Instant.now())
                .build();

        // Send to user and broadcast
        webSocketHandler.sendProgressUpdate(jobId.toString(), userId, update);
        webSocketHandler.broadcastProgress(jobId.toString(), update);

        // Also cache for polling fallback
        pollingFallbackService.cacheProgress(jobId.toString(), update);
    }

    private void sendValidationError(UUID jobId, String userId, int rowNum,
                                     List<ValidationResult.ValidationError> errors) {
        ValidationUpdate.ValidationError[] wsErrors = errors.stream()
                .map(e -> ValidationUpdate.ValidationError.builder()
                        .row(rowNum)
                        .field(e.getField())
                        .message(e.getMessage())
                        .errorCode(e.getErrorCode())
                        .build())
                .toArray(ValidationUpdate.ValidationError[]::new);

        ValidationUpdate update = ValidationUpdate.builder()
                .jobId(jobId.toString())
                .validatedRows(rowNum)
                .newErrors(List.of(wsErrors))
                .complete(false)
                .build();

        webSocketHandler.sendValidationUpdate(jobId.toString(), userId, update);
    }

    private void sendErrorNotification(UUID jobId, String userId, String errorCode,
                                       String message, String entityType, String entityKey, Integer row) {
        MigrationError error = MigrationError.builder()
                .jobId(jobId.toString())
                .errorCode(errorCode)
                .errorMessage(message)
                .entityType(entityType)
                .entityKey(entityKey)
                .row(row)
                .timestamp(Instant.now())
                .severity("ERROR")
                .build();

        webSocketHandler.sendErrorNotification(jobId.toString(), userId, error);
    }

    private void sendJobCompleted(UUID jobId, String userId, int successCount, int failedCount) {
        String status = failedCount > 0 ? "PARTIAL_SUCCESS" : "COMPLETED";

        ImportCompleteNotification notification = ImportCompleteNotification.builder()
                .jobId(jobId.toString())
                .status(status)
                .successCount(successCount)
                .failedCount(failedCount)
                .completedAt(Instant.now())
                .summary(ImportCompleteNotification.ImportSummary.builder()
                        .totalProcessed(successCount + failedCount)
                        .totalFailed(failedCount)
                        .build())
                .build();

        webSocketHandler.sendJobCompleted(jobId.toString(), userId, notification);
    }

    private void sendJobFailed(UUID jobId, String userId, String errorMessage) {
        MigrationError error = MigrationError.builder()
                .jobId(jobId.toString())
                .errorCode("JOB_FAILED")
                .errorMessage(errorMessage)
                .timestamp(Instant.now())
                .severity("CRITICAL")
                .build();

        webSocketHandler.sendErrorNotification(jobId.toString(), userId, error);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseWorkflowMappings(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> out = new HashMap<>();
            map.forEach((k, v) -> out.put(String.valueOf(k), v));
            return out;
        }
        return Map.of();
    }

    private List<Map<String, String>> applyOptionAndStatusMappings(
            UUID jobId,
            List<Map<String, String>> rows,
            Map<String, Object> workflowMappings,
            List<com.avionics_systems.migration.entity.OptionMapping> optionMappings) {

        List<Map<String, String>> result = new ArrayList<>(rows.size());
        for (Map<String, String> row : rows) {
            Map<String, String> copy = new LinkedHashMap<>(row);
            if (copy.containsKey("status")) {
                copy.put("status", workflowStatusMappingService.resolveStatus(
                        workflowMappings, copy.get("status")));
            }
            for (com.avionics_systems.migration.entity.OptionMapping om : optionMappings) {
                String fieldKey = om.getSourceFieldKey().toLowerCase(Locale.ROOT);
                if (copy.containsKey(fieldKey)) {
                    copy.put(fieldKey, optionMappingService.resolveOptionValue(
                            jobId, om.getSourceFieldKey(), copy.get(fieldKey), List.of(om)));
                }
            }
            result.add(copy);
        }
        return result;
    }

    private void resolveAssigneeReporterUsers(List<Map<String, String>> rows, UUID jobId) {
        Set<String> identifiers = new LinkedHashSet<>();
        for (Map<String, String> row : rows) {
            addIfPresent(identifiers, row.get("assignee"));
            addIfPresent(identifiers, row.get("reporter"));
        }
        if (!identifiers.isEmpty()) {
            try {
                userDirectoryMappingService.resolveSourceUsers(identifiers, jobId);
            } catch (Exception e) {
                log.warn("Optional assignee/reporter pre-resolve skipped (import continues): {}", e.getMessage());
            }
        }
    }

    private void addIfPresent(Set<String> set, String value) {
        if (value != null && !value.isBlank()) {
            set.add(value.trim());
        }
    }

    private void persistIssueLinksPass(List<Map<String, Object>> issueRows, UUID jobId) {
        for (Map<String, Object> row : issueRows) {
            String childKey = stringVal(row.get("issueKey"));
            if (childKey == null) {
                continue;
            }
            String parentKey = stringVal(row.get("parentIssueKey"));
            if (parentKey != null && !parentKey.isBlank()) {
                try {
                    issueLinkPersisterHandler.persistParentChild(childKey, parentKey, jobId);
                } catch (Exception e) {
                    log.warn("Parent link failed {} -> {}: {}", childKey, parentKey, e.getMessage());
                }
            }
            String epicKey = stringVal(row.get("epicLink"));
            if (epicKey != null && !epicKey.isBlank()) {
                try {
                    issueLinkPersisterHandler.persistEpicLink(childKey, epicKey, jobId);
                } catch (Exception e) {
                    log.warn("Epic link failed {} -> {}: {}", childKey, epicKey, e.getMessage());
                }
            }
        }
    }

    private void recordIssueResults(UUID jobId, IssuePersisterHandler.BatchPersistResult batchResult) {
        for (IssuePersisterHandler.IssuePersisterResult success : batchResult.getSuccesses()) {
            String sourceKey = success.getSourceIssueKey() != null
                    ? success.getSourceIssueKey() : success.getIssueKey();
            migrationIssueResultService.recordSuccess(
                    jobId,
                    sourceKey,
                    success,
                    success.getRowNumber());
        }
        for (IssuePersisterHandler.IssuePersisterResult failure : batchResult.getFailures()) {
            String sourceKey = failure.getSourceIssueKey() != null
                    ? failure.getSourceIssueKey()
                    : (failure.getIssueKey() != null ? failure.getIssueKey() : "unknown");
            migrationIssueResultService.recordFailure(
                    jobId,
                    sourceKey,
                    failure.getErrorMessage(),
                    failure.getRowNumber());
        }
    }

    private void updateStageProgress(UUID jobId, String stage, int completed, int total) {
        migrationJobRepository.findById(jobId).ifPresent(job -> {
            Map<String, Object> meta = job.getResultMetadata() != null
                    ? new HashMap<>(job.getResultMetadata()) : new HashMap<>();
            Map<String, Object> stages = meta.get("stages") instanceof Map<?, ?> m
                    ? new HashMap<>((Map<String, Object>) m)
                    : new HashMap<>();
            stages.put(stage, Map.of("completed", completed, "total", total));
            meta.put("stages", stages);
            job.setResultMetadata(meta);
            migrationJobRepository.save(job);
        });
    }

    private String stringVal(Object o) {
        return o == null ? null : o.toString().trim();
    }

    /** True when any key is present with non-blank text (avoids Legacy CSV "Comment" column false positives). */
  private static boolean isProjectCsvRow(Map<String, String> row) {
        return hasNonBlank(row, "project_key")
                && hasNonBlank(row, "name")
                && !hasNonBlank(row, "summary", "issue_type", "issue_key", "issuekey");
    }

    private static boolean hasNonBlank(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String v = row.get(key);
            if (v != null && !v.isBlank()) {
                return true;
            }
        }
        return false;
    }
}