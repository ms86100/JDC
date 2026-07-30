package com.avionics_systems.migration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.migration.dto.*;
import com.avionics_systems.migration.entity.CsvTemplate;
import com.avionics_systems.migration.entity.FieldMapping;
import com.avionics_systems.migration.parser.CsvParser;
import com.avionics_systems.migration.parser.LegacyDcXmlParser;
import com.avionics_systems.migration.parser.ValidationEngine;
import com.avionics_systems.migration.repository.CsvTemplateRepository;
import com.avionics_systems.migration.repository.FieldMappingRepository;
import com.avionics_systems.migration.service.MigrationAuditPersistenceService;
import com.avionics_systems.migration.service.MigrationReportService;
import com.avionics_systems.migration.service.MigrationRollbackService;
import com.avionics_systems.migration.service.MigrationJobControlService;
import com.avionics_systems.migration.service.MigrationService;
import com.avionics_systems.migration.service.VirusScanService;
import com.avionics_systems.migration.service.TransactionManager;
import com.avionics_systems.migration.service.ValidationReportService;
import com.avionics_systems.migration.batch.DeadLetterQueueService;
import com.avionics_systems.migration.async.ImportJobProcessor;
import com.avionics_systems.migration.async.ExportJobProcessor;
import com.avionics_systems.migration.dc.LegacyDcImportApiService;
import com.avionics_systems.migration.dc.LegacyDcEnterpriseReadinessService;
import com.avionics_systems.migration.dc.LegacyDcImportOrchestrator;
import com.avionics_systems.migration.dto.JiraDcApiImportRequest;
import com.avionics_systems.migration.jiradc.JiraDcConnectionConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.*;

@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
@Slf4j
public class MigrationController {

    private final MigrationService migrationService;
    private final MigrationJobControlService jobControlService;
    private final VirusScanService virusScanService;
    private final MigrationReportService migrationReportService;
    private final ValidationReportService validationReportService;
    private final MigrationAuditPersistenceService migrationAuditPersistenceService;
    private final MigrationRollbackService migrationRollbackService;
    private final DeadLetterQueueService deadLetterQueueService;
    private final ImportJobProcessor importJobProcessor;
    private final ExportJobProcessor exportJobProcessor;
    private final CsvTemplateRepository csvTemplateRepository;
    private final FieldMappingRepository fieldMappingRepository;
    private final ValidationEngine validationEngine;
    private final CsvParser csvParser;
    private final ObjectMapper objectMapper;
    private final LegacyDcXmlParser legacyDcXmlParser;
    private final LegacyDcImportApiService legacyDcImportApiService;
    private final LegacyDcImportOrchestrator legacyDcImportOrchestrator;
    private final LegacyDcEnterpriseReadinessService legacyDcEnterpriseReadinessService;

    // ============================================
    // MIGRATION JOB MANAGEMENT
    // ============================================

    @PostMapping("/import/csv")
    public ResponseEntity<MigrationJobResponse> startCsvImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "templateId", required = false) UUID templateId,
            @RequestParam(value = "targetProjectId", required = false) UUID targetProjectId,
            @RequestParam(value = "options", required = false) String optionsJson,
            @RequestParam(value = "fieldMappings", required = false) String fieldMappingsJson,
            @RequestHeader("X-User-Id") UUID userId) throws Exception {

        log.info("Starting CSV import: file={}, template={}, project={}",
                file.getOriginalFilename(), templateId, targetProjectId);

        // Read file content BEFORE async processing to avoid temp file cleanup issues
        byte[] fileContent = file.getBytes();
        String fileName = file.getOriginalFilename();

        Map<String, Object> options = optionsJson != null ? parseJson(optionsJson) : new HashMap<>();
        if (fieldMappingsJson != null && !fieldMappingsJson.isBlank()) {
            options.put("fieldMappings", objectMapper.readValue(fieldMappingsJson, List.class));
        }
        options.put("blockOnValidationErrors", true);
        if (targetProjectId != null) {
            options.put("targetProjectId", targetProjectId.toString());
        }

        StartMigrationRequest request = StartMigrationRequest.builder()
                .jobType("IMPORT")
                .importSource("CSV")
                .targetProjectId(targetProjectId)
                .templateId(templateId)
                .options(options)
                .build();

        MigrationJobResponse job = migrationService.startImport(request, userId);

        // Start async processing with file content instead of MultipartFile
        importJobProcessor.processCsvImport(job.getId(), fileContent, fileName, templateId, options, userId);

        return ResponseEntity.accepted().body(job);
    }

    @PostMapping("/import/legacy-dc/validate")
    public ResponseEntity<Map<String, Object>> validateLegacyDcXml(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "attachmentBundle", required = false) MultipartFile attachmentBundle,
            @RequestParam(value = "backupZip", required = false, defaultValue = "false") boolean backupZip,
            @RequestParam(value = "options", required = false) String optionsJson) throws Exception {

        java.nio.file.Path xmlOrZip = java.nio.file.Files.createTempFile("dc-validate-", suffix(file));
        java.nio.file.Files.write(xmlOrZip, file.getBytes());
        java.nio.file.Path bundlePath = null;
        if (attachmentBundle != null && !attachmentBundle.isEmpty()) {
            bundlePath = java.nio.file.Files.createTempFile("dc-att-bundle-", ".zip");
            java.nio.file.Files.write(bundlePath, attachmentBundle.getBytes());
        }
        try {
            Map<String, Object> options = optionsJson != null ? parseJson(optionsJson) : new HashMap<>();
            boolean isBackup = backupZip || file.getOriginalFilename() != null
                    && file.getOriginalFilename().toLowerCase().endsWith(".zip");
            return ResponseEntity.ok(legacyDcImportApiService.validateUpload(
                    xmlOrZip, isBackup, bundlePath, options));
        } finally {
            java.nio.file.Files.deleteIfExists(xmlOrZip);
            if (bundlePath != null) {
                java.nio.file.Files.deleteIfExists(bundlePath);
            }
        }
    }

    @PostMapping("/import/legacy-dc")
    public ResponseEntity<MigrationJobResponse> startLegacyDcImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "attachmentBundle", required = false) MultipartFile attachmentBundle,
            @RequestParam(value = "backupZip", required = false, defaultValue = "false") boolean backupZip,
            @RequestParam(value = "targetProjectId", required = false) UUID targetProjectId,
            @RequestParam(value = "options", required = false) String optionsJson,
            @RequestHeader("X-User-Id") UUID userId) throws Exception {

        log.info("Starting Legacy DC import: file={}", file.getOriginalFilename());

        Map<String, Object> dcOptions = optionsJson != null ? parseJson(optionsJson) : new HashMap<>();
        dcOptions.putIfAbsent("rollbackOnFailure", true);

        java.nio.file.Path uploadPath = java.nio.file.Files.createTempFile("dc-import-", suffix(file));
        java.nio.file.Files.write(uploadPath, file.getBytes());

        boolean isBackup = backupZip || file.getOriginalFilename() != null
                && file.getOriginalFilename().toLowerCase().endsWith(".zip");

        java.nio.file.Path bundleZipPath = null;
        if (attachmentBundle != null && !attachmentBundle.isEmpty()) {
            bundleZipPath = java.nio.file.Files.createTempFile("dc-att-", ".zip");
            java.nio.file.Files.write(bundleZipPath, attachmentBundle.getBytes());
        }

        LegacyDcImportOrchestrator.ResolvedInputs resolved = legacyDcImportOrchestrator.resolveInputs(
                uploadPath, bundleZipPath, isBackup);
        dcOptions.put("xmlPath", resolved.xmlPath().toString());
        if (resolved.attachmentBundlePath() != null) {
            dcOptions.put("attachmentBundlePath", resolved.attachmentBundlePath().toString());
        }
        if (resolved.extractedBackup() != null) {
            dcOptions.put("extractedBackupRoot", resolved.extractedBackup().extractRoot().toString());
        }
        dcOptions.put("backupZip", isBackup);

        String importSource = "issue-xml".equals(String.valueOf(dcOptions.get("importProfile")))
                ? "ISSUE_XML" : "LEGACY_DC";

        StartMigrationRequest request = StartMigrationRequest.builder()
                .jobType("IMPORT")
                .importSource(importSource)
                .targetProjectId(targetProjectId)
                .options(dcOptions)
                .build();

        MigrationJobResponse job = migrationService.startImport(request, userId);

        byte[] fileContent = java.nio.file.Files.readAllBytes(resolved.xmlPath());
        String fileName = resolved.xmlPath().getFileName().toString();

        importJobProcessor.processLegacyDcImport(job.getId(), fileContent, fileName, dcOptions, userId);

        return ResponseEntity.accepted().body(job);
    }

    private static String suffix(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) {
            return name.substring(name.lastIndexOf('.'));
        }
        return ".xml";
    }

    @PostMapping("/import/project")
    public ResponseEntity<MigrationJobResponse> startProjectImport(
            @RequestParam UUID sourceProjectId,
            @RequestParam UUID targetProjectId,
            @RequestParam(value = "options", required = false) String optionsJson,
            @RequestHeader("X-User-Id") UUID userId) {

        log.info("Starting project import: {} -> {}", sourceProjectId, targetProjectId);

        StartMigrationRequest request = StartMigrationRequest.builder()
                .jobType("IMPORT")
                .importSource("PROJECT")
                .sourceProjectId(sourceProjectId)
                .targetProjectId(targetProjectId)
                .build();

        MigrationJobResponse job = migrationService.startImport(request, userId);

        // Start async processing
        importJobProcessor.processProjectImport(job.getId(), sourceProjectId, targetProjectId,
                optionsJson != null ? parseJson(optionsJson) : Map.of(), userId);

        return ResponseEntity.accepted().body(job);
    }

    @PostMapping("/import/jira-dc-api/test-connection")
    public ResponseEntity<Map<String, Object>> testJiraDcConnection(
            @RequestBody Map<String, Object> request) {

        String baseUrl = (String) request.get("jiraBaseUrl");
        String pat = (String) request.get("pat");
        boolean trustAll = Boolean.TRUE.equals(request.get("trustAllCertificates"));

        Map<String, Object> result = new HashMap<>();
        try {
            JiraDcConnectionConfig config = JiraDcConnectionConfig.builder()
                    .baseUrl(baseUrl)
                    .pat(pat)
                    .trustAllCertificates(trustAll)
                    .build();

            com.avionics_systems.migration.jiradc.JiraDcRestClient client =
                    new com.avionics_systems.migration.jiradc.JiraDcRestClient(config, objectMapper);

            Map<String, Object> serverInfo = client.getServerInfo();
            Map<String, Object> myself = client.getMyself();

            @SuppressWarnings("unchecked")
            List<String> projectKeys = (List<String>) request.get("projectKeys");
            String jql = "";
            if (projectKeys != null && !projectKeys.isEmpty()) {
                jql = "project IN (" + String.join(",", projectKeys) + ") ORDER BY key ASC";
            } else {
                jql = "ORDER BY key ASC";
            }
            com.avionics_systems.migration.jiradc.JiraDcRestClient.SearchResult search =
                    client.searchIssues(jql, 0, 1, null);

            List<Map<String, Object>> projects = client.getProjects();
            List<Map<String, String>> projectList = projects.stream()
                    .map(p -> {
                        Map<String, String> proj = new java.util.LinkedHashMap<>();
                        proj.put("key", p.get("key") != null ? p.get("key").toString() : "");
                        proj.put("name", p.get("name") != null ? p.get("name").toString() : "");
                        return proj;
                    })
                    .toList();

            result.put("connected", true);
            result.put("jiraVersion", serverInfo.get("version"));
            result.put("userName", myself.get("displayName"));
            result.put("projectCount", projectList.size());
            result.put("projects", projectList);
            result.put("issueCount", search.total());
        } catch (Exception e) {
            log.warn("DC API connection test failed: {}", e.getMessage());
            result.put("connected", false);
            result.put("error", e.getMessage() != null ? e.getMessage() : "Connection failed");
        }
        return ResponseEntity.ok(result);
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/import/jira-dc-api/discover-fields")
    public ResponseEntity<Map<String, Object>> discoverJiraDcFields(
            @RequestBody Map<String, Object> request) {

        String baseUrl = (String) request.get("jiraBaseUrl");
        String pat = (String) request.get("pat");
        boolean trustAll = Boolean.TRUE.equals(request.get("trustAllCertificates"));

        JiraDcConnectionConfig config = JiraDcConnectionConfig.builder()
                .baseUrl(baseUrl).pat(pat).trustAllCertificates(trustAll).build();

        com.avionics_systems.migration.jiradc.JiraDcRestClient client =
                new com.avionics_systems.migration.jiradc.JiraDcRestClient(config, objectMapper);

        List<Map<String, Object>> jiraFields = client.getFields();
        List<Map<String, Object>> sourceFields = new java.util.ArrayList<>();
        for (Map<String, Object> f : jiraFields) {
            String id = f.get("id") != null ? f.get("id").toString() : null;
            String name = f.get("name") != null ? f.get("name").toString() : id;
            boolean custom = Boolean.TRUE.equals(f.get("custom"));
            if (id == null) continue;
            Map<String, Object> sf = new java.util.LinkedHashMap<>();
            sf.put("key", id);
            sf.put("name", name);
            sf.put("custom", custom);
            Map<String, Object> schema = f.get("schema") instanceof Map<?,?> m
                    ? (Map<String, Object>) m : null;
            sf.put("type", schema != null && schema.get("type") != null
                    ? schema.get("type").toString() : "string");
            sourceFields.add(sf);
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("sourceFields", sourceFields);
        result.put("totalFields", sourceFields.size());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/import/jira-dc-api")
    public ResponseEntity<MigrationJobResponse> startJiraDcApiImport(
            @Valid @RequestBody JiraDcApiImportRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        log.info("Starting Jira DC API import from {}", request.getJiraBaseUrl());

        Map<String, Object> options = request.getOptions() != null ? new HashMap<>(request.getOptions()) : new HashMap<>();
        options.put("jiraBaseUrl", request.getJiraBaseUrl());
        options.put("includeComments", request.isIncludeComments());
        options.put("includeAttachments", request.isIncludeAttachments());
        options.put("includeWorklogs", request.isIncludeWorklogs());
        options.put("includeChangelog", request.isIncludeChangelog());
        if (request.getTargetProjectId() != null) {
            options.put("targetProjectId", request.getTargetProjectId().toString());
        }

        StartMigrationRequest migrationRequest = StartMigrationRequest.builder()
                .jobType("IMPORT")
                .importSource("JIRA_DC_API")
                .targetProjectId(request.getTargetProjectId())
                .options(options)
                .build();

        MigrationJobResponse job = migrationService.startImport(migrationRequest, userId);

        JiraDcConnectionConfig config = JiraDcConnectionConfig.builder()
                .baseUrl(request.getJiraBaseUrl())
                .pat(request.getPat())
                .projectKeys(request.getProjectKeys())
                .jqlFilter(request.getJqlFilter())
                .maxResults(request.getMaxResults())
                .includeComments(request.isIncludeComments())
                .includeAttachments(request.isIncludeAttachments())
                .includeWorklogs(request.isIncludeWorklogs())
                .includeChangelog(request.isIncludeChangelog())
                .trustAllCertificates(request.isTrustAllCertificates())
                .build();

        importJobProcessor.processJiraDcApiImport(job.getId(), config, options, userId);

        return ResponseEntity.accepted().body(job);
    }

    @PostMapping("/export/project")
    public ResponseEntity<MigrationJobResponse> startProjectExport(
            @RequestParam UUID projectId,
            @RequestParam(defaultValue = "xml") String format,
            @RequestParam(value = "options", required = false) String optionsJson,
            @RequestHeader("X-User-Id") UUID userId) {

        log.info("Starting project export: project={}, format={}", projectId, format);

        StartMigrationRequest request = StartMigrationRequest.builder()
                .jobType("EXPORT")
                .sourceProjectId(projectId)
                .build();

        MigrationJobResponse job = migrationService.startImport(request, userId);

        // Start async processing
        exportJobProcessor.exportProject(job.getId(), projectId, format,
                optionsJson != null ? parseJson(optionsJson) : Map.of(), userId);

        return ResponseEntity.accepted().body(job);
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<MigrationJobResponse> getJobStatus(@PathVariable UUID jobId) {
        return ResponseEntity.ok(migrationService.getJobStatus(jobId));
    }

    @GetMapping("/jobs/{jobId}/progress")
    public ResponseEntity<JobProgressResponse> getJobProgress(@PathVariable UUID jobId) {
        return ResponseEntity.ok(migrationService.getJobProgress(jobId));
    }

    @GetMapping("/jobs/{jobId}/result")
    public ResponseEntity<ImportResultResponse> getJobResult(@PathVariable UUID jobId) {
        return ResponseEntity.ok(migrationService.getImportResult(jobId));
    }

    @GetMapping("/jobs/{jobId}/dc-sla-proof")
    public ResponseEntity<Map<String, Object>> getDcSlaProof(@PathVariable UUID jobId) {
        return ResponseEntity.ok(legacyDcEnterpriseReadinessService.getSlaProof(jobId));
    }

    @GetMapping("/jobs/{jobId}/dc-ac-signoff")
    public ResponseEntity<Map<String, Object>> getDcAcSignoff(@PathVariable UUID jobId) {
        return ResponseEntity.ok(legacyDcEnterpriseReadinessService.getAcSignoff(jobId));
    }

    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<Void> cancelJob(
            @PathVariable UUID jobId,
            @RequestHeader("X-User-Id") UUID userId) {
        migrationService.cancelJob(jobId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/jobs/{jobId}/pause")
    public ResponseEntity<Map<String, Object>> pauseJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(jobControlService.pauseJob(jobId));
    }

    @PostMapping("/jobs/{jobId}/resume-control")
    public ResponseEntity<Map<String, Object>> resumePausedJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(jobControlService.resumeJob(jobId));
    }

    @PostMapping("/uploads/{uploadId}/virus-scan")
    public ResponseEntity<Map<String, String>> scanUpload(@PathVariable UUID uploadId) {
        String status = virusScanService.scanAndUpdate(uploadId);
        return ResponseEntity.ok(Map.of("uploadId", uploadId.toString(), "virusScanStatus", status));
    }

    @PostMapping("/jobs/{jobId}/retry")
    public ResponseEntity<Map<String, Object>> retryFailedJob(@PathVariable UUID jobId) {
        List<DeadLetterQueueService.FailedOperation> pending =
                deadLetterQueueService.getByJobId(jobId.toString());
        int retried = 0;
        int succeeded = 0;
        for (DeadLetterQueueService.FailedOperation op : pending) {
            if (op.getId() != null) {
                DeadLetterQueueService.RetryResult result = deadLetterQueueService.retry(op.getId());
                retried++;
                if (result.isSuccess()) {
                    succeeded++;
                }
            }
        }
        return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "retried", retried,
                "succeeded", succeeded,
                "pending", pending.size() - succeeded
        ));
    }

    @GetMapping("/jobs/{jobId}/report")
    public ResponseEntity<String> downloadJobReport(@PathVariable UUID jobId) {
        String csv = migrationReportService.buildImportReportCsv(jobId);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=\"migration-report-" + jobId + ".csv\"")
                .body(csv);
    }

    @GetMapping("/jobs/{jobId}/logs/download")
    public ResponseEntity<String> downloadJobLogs(@PathVariable UUID jobId) {
        return ResponseEntity.ok()
                .header("Content-Type", "text/plain")
                .body(migrationReportService.buildJobLogsText(jobId));
    }

    @GetMapping("/jobs/{jobId}/validation-report")
    public ResponseEntity<String> downloadValidationReport(@PathVariable UUID jobId) {
        String csv = validationReportService.buildValidationReportCsv(jobId, null);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=\"validation-report-" + jobId + ".csv\"")
                .body(csv);
    }

    @GetMapping("/jobs/{jobId}/audit")
    public ResponseEntity<List<?>> getJobAuditTrail(@PathVariable UUID jobId) {
        return ResponseEntity.ok(migrationAuditPersistenceService.getJobTrail(jobId));
    }

    @GetMapping("/jobs/{jobId}/rollback-info")
    public ResponseEntity<TransactionManager.RollbackInfo> getRollbackInfo(@PathVariable UUID jobId) {
        return ResponseEntity.ok(migrationRollbackService.getRollbackInfo(jobId));
    }

    @PostMapping("/jobs/{jobId}/rollback")
    public ResponseEntity<TransactionManager.RollbackResult> rollbackJob(
            @PathVariable UUID jobId,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Rollback job {} requested by {}", jobId, userId);
        TransactionManager.RollbackResult result = migrationRollbackService.rollbackJob(jobId);
        migrationAuditPersistenceService.log(jobId, "ROLLBACK_REQUESTED", "JOB", jobId.toString(), userId,
                Map.of("rolledBack", result.getRolledBackCount(), "failed", result.getFailedCount()));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/jobs")
    public ResponseEntity<Page<MigrationJobResponse>> listJobs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "initiatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        PageRequest pageRequest = PageRequest.of(
                page, Math.min(size, 100),
                Sort.by(Sort.Direction.fromString(sortDir), sortBy)
        );

        Page<MigrationJobResponse> jobs = migrationService.listJobs(status, type, userId, pageRequest);
        return ResponseEntity.ok(jobs);
    }

    // ============================================
    // CSV TEMPLATES
    // ============================================

    @GetMapping("/templates")
    public ResponseEntity<List<CsvTemplate>> listTemplates(
            @RequestParam(required = false) String entityType) {
        List<CsvTemplate> templates;
        if (entityType != null) {
            templates = csvTemplateRepository.findByEntityType(entityType);
        } else {
            templates = csvTemplateRepository.findAll();
        }
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/templates/{templateId}")
    public ResponseEntity<CsvTemplate> getTemplate(@PathVariable UUID templateId) {
        return csvTemplateRepository.findById(templateId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/templates/{templateId}/download")
    public ResponseEntity<String> downloadTemplate(@PathVariable UUID templateId) {
        return csvTemplateRepository.findById(templateId)
                .map(template -> {
                    String csv = generateTemplateCsv(template);
                    return ResponseEntity.ok()
                            .header("Content-Type", "text/csv")
                            .header("Content-Disposition", "attachment; filename=\"" + template.getTemplateName() + ".csv\"")
                            .body(csv);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private String generateTemplateCsv(CsvTemplate template) {
        StringBuilder csv = new StringBuilder();

        try {
            List<Map<String, Object>> columns = objectMapper.readValue(
                    template.getColumns(),
                    new com.fasterxml.jackson.core.type.TypeReference<>() {}
            );

            // Generate header row
            csv.append("row_number,");
            for (Map<String, Object> col : columns) {
                csv.append(col.get("display_name")).append(",");
            }
            csv.append("\n");

            // Generate sample row
            csv.append("1,");
            for (Map<String, Object> col : columns) {
                String dataType = (String) col.getOrDefault("data_type", "VARCHAR");
                String sample = getSampleValue(dataType);
                csv.append(sample).append(",");
            }

        } catch (Exception e) {
            log.error("Failed to generate template CSV", e);
        }

        return csv.toString();
    }

    private String getSampleValue(String dataType) {
        return switch (dataType.toUpperCase()) {
            case "INTEGER", "INT" -> "1";
            case "BOOLEAN" -> "true";
            case "DATE" -> "2026-01-01";
            case "DATETIME" -> "2026-01-01T00:00:00";
            default -> "sample_value";
        };
    }

    // ============================================
    // FIELD MAPPINGS
    // ============================================

    @GetMapping("/mappings")
    public ResponseEntity<List<FieldMapping>> listMappings(
            @RequestParam(required = false) String mappingType) {
        List<FieldMapping> mappings;
        if (mappingType != null) {
            mappings = fieldMappingRepository.findByMappingType(mappingType);
        } else {
            mappings = fieldMappingRepository.findAll();
        }
        return ResponseEntity.ok(mappings);
    }

    @PostMapping("/mappings")
    public ResponseEntity<FieldMapping> createMapping(@RequestBody FieldMapping mapping) {
        FieldMapping saved = fieldMappingRepository.save(mapping);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/mappings/{mappingId}")
    public ResponseEntity<FieldMapping> getMapping(@PathVariable UUID mappingId) {
        return fieldMappingRepository.findById(mappingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/mappings/{mappingId}")
    public ResponseEntity<Void> deleteMapping(@PathVariable UUID mappingId) {
        fieldMappingRepository.deleteById(mappingId);
        return ResponseEntity.noContent().build();
    }

    // ============================================
    // VALIDATION
    // ============================================

    @PostMapping("/validate/csv")
    public ResponseEntity<ValidationResult> validateCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam String entityType,
            @RequestParam(required = false) UUID templateId) {

        try {
            List<Map<String, String>> rows = parseMultipartCsv(file);
            List<String> errors = validationEngine.validateBulk(rows, entityType);

            if (errors.isEmpty()) {
                return ResponseEntity.ok(ValidationResult.success());
            } else {
                return ResponseEntity.badRequest().body(
                        ValidationResult.builder()
                                .valid(false)
                                .errors(errors.stream()
                                        .map(e -> ValidationResult.ValidationError.builder()
                                                .errorCode("VALIDATION_ERROR")
                                                .message(e)
                                                .build())
                                        .toList())
                                .warnings(List.of())
                                .build()
                );
            }
        } catch (Exception e) {
            log.error("Validation failed", e);
            return ResponseEntity.badRequest().body(
                    ValidationResult.withErrors(List.of(
                            ValidationResult.ValidationError.builder()
                                    .errorCode("VALIDATION_FAILED")
                                    .message(e.getMessage())
                                    .build()
                    ))
            );
        }
    }

    @PostMapping("/validate/row")
    public ResponseEntity<ValidationResult> validateRow(
            @RequestBody Map<String, String> row,
            @RequestParam String entityType) {

        ValidationResult result = validationEngine.validateRow(row, entityType, 1);

        if (result.isValid()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse options JSON", e);
            return Map.of();
        }
    }

    private List<Map<String, String>> parseMultipartCsv(MultipartFile file) throws Exception {
        CsvParser.CsvParseResult result = csvParser.parseContent(
                new String(file.getBytes()),
                1
        );

        List<Map<String, String>> rows = new ArrayList<>();
        for (String[] row : result.getDataRows()) {
            Map<String, String> rowMap = new HashMap<>();
            for (int i = 0; i < result.getHeaders().length && i < row.length; i++) {
                rowMap.put(result.getHeaders()[i].trim().toLowerCase(), row[i]);
            }
            rows.add(rowMap);
        }
        return rows;
    }
}