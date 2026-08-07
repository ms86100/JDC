package com.avionics_systems.migration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.migration.async.ImportJobProcessor;
import com.avionics_systems.migration.dc.LegacyDcImportApiService;
import com.avionics_systems.migration.dto.MigrationJobResponse;
import com.avionics_systems.migration.dto.StartMigrationRequest;
import com.avionics_systems.migration.dto.ValidationResult;
import com.avionics_systems.migration.dto.wizard.*;
import com.avionics_systems.migration.entity.MigrationFileUpload;
import com.avionics_systems.migration.entity.WizardSession;
import com.avionics_systems.migration.exception.EntityNotFoundException;
import com.avionics_systems.migration.parser.CsvParser;
import com.avionics_systems.migration.parser.ImportSpreadsheetParser;
import com.avionics_systems.migration.parser.ValidationEngine;
import com.avionics_systems.migration.repository.MigrationFileUploadRepository;
import com.avionics_systems.migration.repository.MigrationJobRepository;
import com.avionics_systems.migration.repository.WizardSessionRepository;
import com.avionics_systems.migration.entity.MigrationJob;
import com.avionics_systems.migration.service.clients.ProjectServiceClient;
import com.avionics_systems.migration.service.clients.dto.ProjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportWizardSessionService {

    @org.springframework.beans.factory.annotation.Value("${app.wizard.preview-row-limit:25}")
    private int previewRowLimit;

    @org.springframework.beans.factory.annotation.Value("${app.wizard.session-ttl-hours:24}")
    private int sessionTtlHours;

    @org.springframework.beans.factory.annotation.Value("${app.wizard.max-upload-size-mb:500}")
    private long maxUploadSizeMb;

    @org.springframework.beans.factory.annotation.Value("${app.wizard.supported-formats:CSV, XLSX (Excel), XML/ZIP (Legacy DC)}")
    private String supportedFormats;

    @org.springframework.beans.factory.annotation.Value("${app.wizard.default-import-type:CSV}")
    private String defaultImportType;

    private final WizardSessionRepository wizardSessionRepository;
    private final MigrationFileUploadRepository fileUploadRepository;
    private final MigrationService migrationService;
    private final ImportJobProcessor importJobProcessor;
    private final ValidationEngine validationEngine;
    private final CsvParser csvParser;
    private final ImportSpreadsheetParser importSpreadsheetParser;
    private final ObjectMapper objectMapper;
    private final MigrationJobRepository migrationJobRepository;
    private final OptionMappingService optionMappingService;
    private final VirusScanService virusScanService;
    private final DryRunValidationService dryRunValidationService;
    private final TargetProjectValidator targetProjectValidator;
    private final LegacyDcImportApiService legacyDcImportApiService;
    private final ProjectServiceClient projectServiceClient;
    private final com.avionics_systems.migration.service.field.FieldDiscoveryService fieldDiscoveryService;
    private final com.avionics_systems.migration.service.field.FieldProvisioningService fieldProvisioningService;

    @Transactional
    public WizardSessionDto createSession(CreateWizardSessionRequest request, UUID userId) {
        String importType = normalizeImportType(request.getImportType());

        WizardSession session = WizardSession.builder()
                .importType(importType)
                .currentStep("UPLOAD")
                .status("IN_PROGRESS")
                .targetProjectId(request.getTargetProjectId())
                .initiatedBy(userId)
                .importOptions(request.getOptions() != null ? request.getOptions() : new HashMap<>())
                .sessionData(new HashMap<>())
                .expiresAt(LocalDateTime.now().plusHours(sessionTtlHours))
                .build();

        session = wizardSessionRepository.save(session);
        log.info("Created wizard session {} type={}", session.getId(), importType);
        return toDto(session);
    }

    @Transactional(readOnly = true)
    public WizardSessionDto getSession(UUID sessionId) {
        WizardSession session = requireSession(sessionId);
        return toDto(session);
    }

    @Transactional
    public WizardSessionDto updateSession(UUID sessionId, UpdateWizardSessionRequest request) {
        WizardSession session = requireSession(sessionId);

        if (request.getStep() != null) {
            session.advanceStep(request.getStep());
        }
        if (request.getTargetProjectId() != null) {
            session.setTargetProjectId(request.getTargetProjectId());
        }
        if (request.getFieldMappings() != null) {
            session.setFieldMappings(request.getFieldMappings());
            if (!"MAP_FIELDS".equals(session.getCurrentStep()) && !"REVIEW".equals(session.getCurrentStep())) {
                session.advanceStep("MAP_FIELDS");
            }
        }
        if (request.getUserMappings() != null) {
            session.setUserMappings(request.getUserMappings());
        }
        if (request.getImportOptions() != null) {
            Map<String, Object> merged = session.getImportOptions() != null
                    ? new HashMap<>(session.getImportOptions()) : new HashMap<>();
            merged.putAll(request.getImportOptions());
            session.setImportOptions(merged);
            if (request.getImportOptions().get("fieldDefaults") instanceof Map<?, ?> defaults) {
                Map<String, Object> fieldDefaults = new HashMap<>();
                defaults.forEach((k, v) -> fieldDefaults.put(String.valueOf(k), v));
                session.setFieldDefaults(fieldDefaults);
            }
            if (request.getImportOptions().get("workflowStatusMappings") instanceof Map<?, ?> wf) {
                Map<String, Object> workflow = new HashMap<>();
                wf.forEach((k, v) -> workflow.put(String.valueOf(k), v));
                session.setWorkflowStatusMappings(workflow);
            }
        }
        if (request.getSessionData() != null) {
            Map<String, Object> merged = session.getSessionData() != null
                    ? new HashMap<>(session.getSessionData()) : new HashMap<>();
            merged.putAll(request.getSessionData());
            session.setSessionData(merged);
        }

        session = wizardSessionRepository.save(session);
        return toDto(session);
    }

    @Transactional(noRollbackFor = {IllegalStateException.class, IllegalArgumentException.class})
    public WizardUploadResultDto uploadFile(UUID sessionId, MultipartFile file, String importTypeOverride, UUID userId) {
        WizardSession session = requireSession(sessionId);
        String importType = importTypeOverride != null ? normalizeImportType(importTypeOverride) : session.getImportType();

        validateFile(file);

        try {
            byte[] content = file.getBytes();
            String checksum = sha256Hex(content);

            fileUploadRepository.findFirstByWizardSessionIdOrderByCreatedAtDesc(sessionId)
                    .ifPresent(existing -> fileUploadRepository.delete(existing));

            MigrationFileUpload upload = MigrationFileUpload.builder()
                    .wizardSessionId(sessionId)
                    .fileName(file.getOriginalFilename())
                    .mimeType(file.getContentType())
                    .fileSize(file.getSize())
                    .checksum(checksum)
                    .fileContent(content)
                    .virusScanStatus("PENDING")
                    .parseStatus("PENDING")
                    .build();
            upload = fileUploadRepository.save(upload);
            virusScanService.scanUploadAsync(upload.getId());

            session.setFileName(file.getOriginalFilename());
            session.setFileSize(file.getSize());
            session.setMimeType(file.getContentType());
            session.setImportType(importType);

            WizardUploadResultDto.WizardUploadResultDtoBuilder result = WizardUploadResultDto.builder()
                    .sessionId(sessionId)
                    .uploadId(upload.getId())
                    .virusScanStatus(upload.getVirusScanStatus())
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .success(true);

            String fileName = file.getOriginalFilename();
            if ("LEGACY_DC".equals(importType) || "ISSUE_XML".equals(importType)
                    || (fileName != null && fileName.toLowerCase().endsWith(".xml"))) {
                parseLegacyDcIntoSession(session, content, result);
            } else {
                parseSpreadsheetIntoSession(session, content, fileName, result);
            }

            // Auto-discover and provision fields from detected headers
            try {
                var headers = session.getDetectedHeaders();
                if (headers != null && !headers.isEmpty()) {
                    var payload = new java.util.LinkedHashMap<String, Object>();
                    headers.forEach(h -> payload.put(h, ""));
                    fieldProvisioningService.initializeBuiltInFields(userId);
                    var discovery = fieldDiscoveryService.discoverFields(payload);
                    var toProvision = discovery.discoveredFields().stream()
                            .filter(f -> f.requiresProvisioning() && !f.isKnown())
                            .toList();
                    if (!toProvision.isEmpty()) {
                        fieldProvisioningService.provisionFields(toProvision, userId);
                        log.info("Auto-provisioned {} custom fields from {} detected headers for session {}",
                                toProvision.size(), headers.size(), sessionId);
                    }
                }
            } catch (Exception discoverEx) {
                log.warn("Auto field discovery failed for session {}: {}", sessionId, discoverEx.getMessage());
            }

            try {
                session.advanceStep("TARGET_PROJECT");
            } catch (Exception stepEx) {
                log.warn("Wizard session {} could not advance to TARGET_PROJECT: {}", sessionId, stepEx.getMessage());
                session.setCurrentStep("UPLOAD");
            }
            upload.setParseStatus("PARSED");
            fileUploadRepository.save(upload);
            wizardSessionRepository.save(session);

            return result.build();
        } catch (Exception e) {
            log.error("Wizard upload failed for session {}", sessionId, e);
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            session.setErrorMessage(message);
            try {
                wizardSessionRepository.save(session);
            } catch (Exception saveEx) {
                log.warn("Could not persist wizard session error for {}: {}", sessionId, saveEx.getMessage());
            }
            return WizardUploadResultDto.builder()
                    .sessionId(sessionId)
                    .fileName(file.getOriginalFilename())
                    .success(false)
                    .errorMessage(message)
                    .build();
        }
    }

    @Transactional
    public ValidationResult validateSession(UUID sessionId, String entityType) {
        WizardSession session = requireSession(sessionId);
        if ("LEGACY_DC".equals(session.getImportType()) || "ISSUE_XML".equals(session.getImportType())) {
            return validateLegacyDcSession(sessionId, session);
        }
        byte[] content = requireFileContent(sessionId);

        CsvParser.CsvParseResult parsed = parseUploadedContent(sessionId, session);
        List<Map<String, String>> rows = toRowMaps(parsed, session);
        String resolvedType = entityType != null ? entityType : session.getDetectedEntityType();
        if (resolvedType == null) {
            resolvedType = detectEntityType(parsed.getHeaders());
        }

        ValidationResult result = dryRunValidationService.validateAndPersist(
                session.getMigrationJobId(),
                sessionId,
                resolvedType,
                rows,
                true);

        session.setValidationResult(Map.of(
                "valid", result.isValid(),
                "errorCount", result.getErrors().size(),
                "warningCount", result.getWarnings().size(),
                "totalRows", rows.size(),
                "entityType", resolvedType
        ));
        session.advanceStep("VALIDATE");
        wizardSessionRepository.save(session);

        return result;
    }

    @Transactional
    public WizardSessionDto getPreview(UUID sessionId, int page, int size) {
        WizardSession session = requireSession(sessionId);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);

        if (session.getPreviewRows() == null || session.getPreviewRows().isEmpty()) {
            WizardSession sessionRef = session;
            CsvParser.CsvParseResult parsed = parseUploadedContent(sessionId, sessionRef);
            List<List<String>> preview = new ArrayList<>();
            preview.add(Arrays.asList(parsed.getHeaders()));
            for (int i = 0; i < Math.min(parsed.getDataRows().size(), previewRowLimit); i++) {
                preview.add(Arrays.asList(parsed.getDataRows().get(i)));
            }
            session.setPreviewRows(preview);
            wizardSessionRepository.save(session);
        }

        List<List<String>> all = session.getPreviewRows();
        int from = safePage * safeSize;
        int to = Math.min(from + safeSize, all.size());
        List<List<String>> pageRows = from < all.size() ? all.subList(from, to) : List.of();

        WizardSessionDto dto = toDto(session);
        dto.setPreviewRows(pageRows);
        Map<String, Object> meta = dto.getSessionData() != null ? dto.getSessionData() : new HashMap<>();
        meta.put("previewPage", safePage);
        meta.put("previewTotalRows", all.size());
        dto.setSessionData(meta);
        return dto;
    }

    @Transactional
    public MigrationJobResponse executeImport(UUID sessionId, WizardExecuteImportRequest request, UUID userId) {
        WizardSession session = requireSession(sessionId);

        if (session.getValidationResult() != null
                && Boolean.FALSE.equals(session.getValidationResult().get("valid"))) {
            throw new IllegalStateException("Cannot execute import: validation errors must be resolved first");
        }
        if (session.getValidationResult() == null
                && !"LEGACY_DC".equals(session.getImportType())) {
            validateSession(sessionId, session.getDetectedEntityType());
            session = requireSession(sessionId);
            if (session.getValidationResult() != null
                    && Boolean.FALSE.equals(session.getValidationResult().get("valid"))) {
                throw new IllegalStateException("Cannot execute import: validation failed");
            }
        }

        UUID targetProjectId = request.getTargetProjectId() != null
                ? request.getTargetProjectId() : session.getTargetProjectId();
        if (targetProjectId != null) {
            targetProjectValidator.assertProjectExists(targetProjectId);
        }

        Map<String, Object> options = new HashMap<>();
        if (session.getImportOptions() != null) {
            options.putAll(session.getImportOptions());
        }
        if (request.getOptions() != null) {
            options.putAll(request.getOptions());
        }
        if (session.getFieldMappings() != null) {
            options.put("fieldMappings", session.getFieldMappings());
        }
        if (session.getOptionMappings() != null) {
            options.put("optionMappings", session.getOptionMappings());
        }
        if (session.getWorkflowStatusMappings() != null) {
            options.put("workflowStatusMappings", session.getWorkflowStatusMappings());
        }
        if (session.getFieldDefaults() != null) {
            options.put("fieldDefaults", session.getFieldDefaults());
        }
        if (session.getUserMappings() != null) {
            options.put("userMappings", session.getUserMappings());
        }
        options.put("blockOnValidationErrors", true);
        options.put("wizardSessionId", sessionId.toString());
        if (targetProjectId != null) {
            options.put("targetProjectId", targetProjectId.toString());
        }

        StartMigrationRequest migrationRequest = StartMigrationRequest.builder()
                .jobType("IMPORT")
                .importSource(session.getImportType())
                .targetProjectId(targetProjectId)
                .options(options)
                .build();

        MigrationJobResponse job = migrationService.startImport(migrationRequest, userId);

        final Map<String, Object> workflowMappings = session.getWorkflowStatusMappings();
        final Map<String, Object> fieldDefaultsMap = session.getFieldDefaults();
        final List<Map<String, Object>> optionMappingsList = session.getOptionMappings();

        migrationJobRepository.findById(job.getId()).ifPresent(migrationJob -> {
            migrationJob.setWorkflowStatusMappings(workflowMappings);
            migrationJob.setFieldDefaults(fieldDefaultsMap);
            migrationJob.setOptionMappings(optionMappingsList);
            migrationJobRepository.save(migrationJob);
        });
        if (optionMappingsList != null && !optionMappingsList.isEmpty()) {
            optionMappingService.saveForJob(job.getId(), optionMappingsList);
        }

        MigrationFileUpload upload = fileUploadRepository
                .findFirstByWizardSessionIdOrderByCreatedAtDesc(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("MigrationFileUpload", sessionId.toString()));

        upload.setMigrationJobId(job.getId());
        fileUploadRepository.save(upload);

        session.setMigrationJobId(job.getId());
        session.setTargetProjectId(targetProjectId);
        session.advanceStep("EXECUTE");
        session.setStatus("IN_PROGRESS");
        wizardSessionRepository.save(session);

        final UUID jobId = job.getId();
        final byte[] fileContent = upload.getFileContent();
        final String fileName = upload.getFileName();
        final String importType = session.getImportType();
        final Map<String, Object> importOptions = new HashMap<>(options);
        final UUID actorId = userId;

        scheduleImportAfterCommit(jobId, fileContent, fileName, importType, importOptions, actorId);

        log.info("Wizard session {} started migration job {}", sessionId, jobId);
        return job;
    }

    @Transactional
    public void markSessionCompleted(UUID sessionId, UUID jobId) {
        wizardSessionRepository.findById(sessionId).ifPresent(session -> {
            session.setMigrationJobId(jobId);
            session.advanceStep("COMPLETED");
            session.setStatus("COMPLETED");
            wizardSessionRepository.save(session);
        });
    }

    /**
     * Run async import only after the wizard execute transaction commits so the worker sees job + file rows.
     */
    private void scheduleImportAfterCommit(
            UUID jobId,
            byte[] fileContent,
            String fileName,
            String importType,
            Map<String, Object> options,
            UUID userId) {
        Runnable task = () -> {
            if ("LEGACY_DC".equals(importType) || "ISSUE_XML".equals(importType)) {
                importJobProcessor.processLegacyDcImport(jobId, fileContent, fileName, options, userId);
            } else {
                importJobProcessor.processSpreadsheetImport(jobId, fileContent, fileName, null, options, userId);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private CsvParser.CsvParseResult parseUploadedContent(UUID sessionId, WizardSession session) {
        byte[] content = requireFileContent(sessionId);
        String fileName = session.getFileName();
        try {
            if (fileName != null && fileName.toLowerCase().endsWith(".xml")) {
                throw new IllegalStateException("Preview not available for XML uploads");
            }
            return importSpreadsheetParser.parse(content, fileName);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse file: " + e.getMessage(), e);
        }
    }

    private void parseSpreadsheetIntoSession(WizardSession session, byte[] content, String fileName,
                                             WizardUploadResultDto.WizardUploadResultDtoBuilder result) {
        CsvParser.CsvParseResult parsed;
        try {
            parsed = importSpreadsheetParser.parse(content, fileName);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse spreadsheet: " + e.getMessage(), e);
        }
        List<String> headers = Arrays.stream(parsed.getHeaders())
                .map(h -> h.trim())
                .collect(Collectors.toList());

        session.setDetectedHeaders(headers);
        session.setDetectedEntityType(detectEntityType(parsed.getHeaders()));
        session.setTotalRows(parsed.getTotalRows());
        detectSpecialColumns(session, headers);

        List<List<String>> preview = new ArrayList<>();
        preview.add(headers);
        for (int i = 0; i < Math.min(parsed.getDataRows().size(), previewRowLimit); i++) {
            preview.add(Arrays.asList(parsed.getDataRows().get(i)));
        }
        session.setPreviewRows(preview);

        result.detectedHeaders(headers)
                .detectedEntityType(session.getDetectedEntityType())
                .attachmentColumn(session.getAttachmentColumn())
                .parentColumn(session.getParentColumn())
                .epicColumn(session.getEpicColumn())
                .totalRows(parsed.getTotalRows())
                .previewRows(preview);
    }

    private void parseLegacyDcIntoSession(WizardSession session, byte[] content,
                                        WizardUploadResultDto.WizardUploadResultDtoBuilder result) {
        String xml = new String(content, StandardCharsets.UTF_8);
        List<String> entities = new ArrayList<>();
        if (xml.contains("<Project")) entities.add("Project");
        if (xml.contains("<Issue")) entities.add("Issue");
        if (xml.contains("<User")) entities.add("User");
        session.setDetectedEntityType("ISSUE");
        session.setDetectedHeaders(entities);
        result.detectedEntityTypes(entities).detectedEntityType("LEGACY_DC");
    }

    private ValidationResult validateLegacyDcSession(UUID sessionId, WizardSession session) {
        byte[] content = requireFileContent(sessionId);
        String fileName = session.getFileName() != null ? session.getFileName() : "upload.xml";
        Path temp = null;
        try {
            String suffix = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : ".xml";
            temp = Files.createTempFile("wizard-dc-validate-", suffix);
            Files.write(temp, content);
            boolean isBackup = fileName.toLowerCase(Locale.ROOT).endsWith(".zip");
            Map<String, Object> options = session.getImportOptions() != null
                    ? session.getImportOptions() : Map.of();
            Map<String, Object> apiResult = legacyDcImportApiService.validateUpload(
                    temp, isBackup, null, options);

            boolean valid = Boolean.TRUE.equals(apiResult.get("valid"));
            List<ValidationResult.ValidationError> errors = new ArrayList<>();
            if (apiResult.get("errors") instanceof List<?> errList) {
                for (Object o : errList) {
                    if (o instanceof Map<?, ?> m) {
                        errors.add(ValidationResult.ValidationError.builder()
                                .row(0)
                                .field(String.valueOf(m.get("field")))
                                .errorCode(String.valueOf(m.get("code")))
                                .message(String.valueOf(m.get("message")))
                                .build());
                    }
                }
            }
            List<ValidationResult.ValidationWarning> warnings = new ArrayList<>();
            if (apiResult.get("warnings") instanceof List<?> warnList) {
                for (Object o : warnList) {
                    if (o instanceof Map<?, ?> m) {
                        warnings.add(ValidationResult.ValidationWarning.builder()
                                .row(0)
                                .field(String.valueOf(m.get("field")))
                                .warningCode(String.valueOf(m.get("code")))
                                .message(String.valueOf(m.get("message")))
                                .build());
                    }
                }
            }
            ValidationResult result = ValidationResult.builder()
                    .valid(valid)
                    .errors(errors)
                    .warnings(warnings)
                    .build();

            dryRunValidationService.persistValidationResult(
                    session.getMigrationJobId(),
                    sessionId,
                    "LEGACY_DC",
                    result,
                    true);

            session.setValidationResult(Map.of(
                    "valid", valid,
                    "errorCount", errors.size(),
                    "warningCount", apiResult.getOrDefault("warningCount", 0),
                    "totalRows", apiResult.getOrDefault("totalEntities", 0),
                    "entityType", "LEGACY_DC",
                    "format", apiResult.get("format"),
                    "riskScore", apiResult.get("riskScore")
            ));
            session.advanceStep("MAP_FIELDS");
            wizardSessionRepository.save(session);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Legacy DC validation failed: " + e.getMessage(), e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }
    }

    private void detectSpecialColumns(WizardSession session, List<String> headers) {
        for (String h : headers) {
            String lower = h.toLowerCase(Locale.ROOT);
            if (lower.contains("attachment") || lower.equals("attachments")) {
                session.setAttachmentColumn(h);
            }
            if (lower.equals("parent") || lower.equals("parent_key") || lower.equals("parent issue")) {
                session.setParentColumn(h);
            }
            if (lower.equals("epic") || lower.equals("epic_link") || lower.equals("epic link")) {
                session.setEpicColumn(h);
            }
        }
    }

    private List<Map<String, String>> toRowMaps(CsvParser.CsvParseResult parsed, WizardSession session) {
        Map<String, String> mappedTargets = buildMappedTargetKeys(session);
        List<Map<String, String>> rows = new ArrayList<>();
        for (String[] row : parsed.getDataRows()) {
            Map<String, String> map = new HashMap<>();
            for (int i = 0; i < parsed.getHeaders().length && i < row.length; i++) {
                String header = parsed.getHeaders()[i];
                String normalizedKey = normalizeHeaderKey(header);
                String value = row[i];
                putRowValue(map, normalizedKey, value);
                String mappedTarget = mappedTargets.get(normalizedKey);
                if (mappedTarget != null && !mappedTarget.isBlank()) {
                    putRowValue(map, mappedTarget, value);
                }
            }
            deriveProjectKeyIfMissing(map, session);
            rows.add(map);
        }
        return rows;
    }

    private void deriveProjectKeyIfMissing(Map<String, String> row, WizardSession session) {
        String projectKey = row.get("project_key");
        if (projectKey != null && !projectKey.isBlank()) {
            return;
        }
        String issueKey = row.get("issue_key");
        if (issueKey != null && issueKey.contains("-")) {
            row.put("project_key", issueKey.substring(0, issueKey.lastIndexOf('-')));
            return;
        }
        if (session.getTargetProjectId() != null) {
            try {
                ProjectResponse project = projectServiceClient.getProject(session.getTargetProjectId().toString());
                if (project != null && project.getKey() != null && !project.getKey().isBlank()) {
                    row.put("project_key", project.getKey());
                }
            } catch (Exception e) {
                log.debug("Could not resolve project key for targetProjectId {}: {}", session.getTargetProjectId(), e.getMessage());
            }
        }
    }

    /** Do not let a later empty mapped column overwrite a non-empty canonical value (e.g. Epic Name → summary). */
    private static void putRowValue(Map<String, String> map, String key, String value) {
        if (key == null || key.isBlank()) {
            return;
        }
        String v = value != null ? value : "";
        String existing = map.get(key);
        if (existing == null || existing.isBlank()) {
            map.put(key, v);
        } else if (!v.isBlank()) {
            map.put(key, v);
        }
    }

    /** "Issue Type" / "Project Key" → issue_type / project_key for ValidationEngine row lookups. */
    private static String normalizeHeaderKey(String header) {
        return header.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s-]+", "_")
                .replaceAll("[^a-z0-9_]", "");
    }

    /** Map UI target field keys (issuetype, project) to validation row keys (issue_type, project_key). */
    private static String canonicalTargetKey(String targetField) {
        String key = normalizeHeaderKey(targetField);
        return switch (key) {
            case "issuetype", "type" -> "issue_type";
            case "project", "proj" -> "project_key";
            case "issuekey", "key" -> "issue_key";
            default -> key;
        };
    }

    private Map<String, String> buildMappedTargetKeys(WizardSession session) {
        if (session.getFieldMappings() == null) {
            return Map.of();
        }
        Map<String, String> lookup = new HashMap<>();
        for (Map<String, Object> mapping : session.getFieldMappings()) {
            Object source = mapping.get("sourceColumn");
            if (source == null) {
                source = mapping.get("sourceField");
            }
            Object target = mapping.get("targetField");
            if (source == null || target == null) {
                continue;
            }
            Boolean mapped = mapping.get("mapped") instanceof Boolean b ? b : Boolean.TRUE;
            if (!mapped) {
                continue;
            }
            lookup.put(
                    normalizeHeaderKey(String.valueOf(source)),
                    canonicalTargetKey(String.valueOf(target)));
        }
        return lookup;
    }

    private String detectEntityType(String[] headers) {
        Set<String> headerSet = new HashSet<>();
        for (String h : headers) {
            headerSet.add(normalizeHeaderKey(h));
        }
        if (headerSet.contains("project_key") && !headerSet.contains("issue_key")) return "PROJECT";
        if (headerSet.contains("issue_key") || headerSet.contains("summary")) return "ISSUE";
        if (headerSet.contains("email") && headerSet.contains("username")) return "USER";
        return "ISSUE";
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        long maxBytes = maxUploadSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File exceeds maximum size of " + maxUploadSizeMb + "MB");
        }
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        boolean csv = name.endsWith(".csv");
        boolean xml = name.endsWith(".xml");
        boolean xlsx = name.endsWith(".xlsx");
        boolean zip = name.endsWith(".zip");
        if (!csv && !xml && !xlsx && !zip) {
            throw new IllegalArgumentException("Supported formats: " + supportedFormats);
        }
        String mime = file.getContentType();
        if (mime != null && !mime.isBlank()) {
            String lower = mime.toLowerCase(Locale.ROOT);
            boolean ok = lower.contains("csv")
                    || lower.contains("spreadsheet")
                    || lower.contains("excel")
                    || lower.contains("xml")
                    || lower.contains("zip")
                    || lower.equals("application/octet-stream")
                    || lower.equals("text/plain")
                    || lower.equals("application/vnd.ms-excel")
                    || lower.startsWith("text/");
            if (!ok && !(csv || xlsx || xml || zip)) {
                throw new IllegalArgumentException("Unsupported MIME type: " + mime);
            }
        }
    }

    private String normalizeImportType(String type) {
        if (type == null) return defaultImportType;
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "csv" -> "CSV";
            case "issue-xml", "issue_xml", "issuexml" -> "ISSUE_XML";
            case "legacy-dc", "legacy_dc", "legacydc" -> "LEGACY_DC";
            case "project", "project-import", "project_import" -> "PROJECT";
            default -> type.toUpperCase(Locale.ROOT);
        };
    }

    private WizardSession requireSession(UUID sessionId) {
        return wizardSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("WizardSession", sessionId.toString()));
    }

    private byte[] requireFileContent(UUID sessionId) {
        return fileUploadRepository.findFirstByWizardSessionIdOrderByCreatedAtDesc(sessionId)
                .map(MigrationFileUpload::getFileContent)
                .orElseThrow(() -> new EntityNotFoundException("MigrationFileUpload", sessionId.toString()));
    }

    private String sha256Hex(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Transactional
    public void updateFieldDefaults(UUID sessionId, Map<String, Object> defaults) {
        WizardSession session = requireSession(sessionId);
        session.setFieldDefaults(defaults != null ? defaults : new HashMap<>());
        wizardSessionRepository.save(session);
    }

    @Transactional
    public void updateOptionMappings(UUID sessionId, List<Map<String, Object>> mappings) {
        WizardSession session = requireSession(sessionId);
        session.setOptionMappings(mappings);
        wizardSessionRepository.save(session);
    }

    @Transactional
    public void updateWorkflowStatusMappings(UUID sessionId, Map<String, Object> mappings) {
        WizardSession session = requireSession(sessionId);
        session.setWorkflowStatusMappings(mappings);
        wizardSessionRepository.save(session);
    }

    private WizardSessionDto toDto(WizardSession session) {
        Map<String, Object> sessionData = session.getSessionData() != null
                ? new HashMap<>(session.getSessionData()) : new HashMap<>();
        sessionData.putIfAbsent("importType", session.getImportType());

        return WizardSessionDto.builder()
                .sessionId(session.getId())
                .step(session.getCurrentStep())
                .importType(session.getImportType())
                .status(session.getStatus())
                .targetProjectId(session.getTargetProjectId())
                .migrationJobId(session.getMigrationJobId())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .fileName(session.getFileName())
                .fileSize(session.getFileSize())
                .detectedHeaders(session.getDetectedHeaders())
                .detectedEntityType(session.getDetectedEntityType())
                .attachmentColumn(session.getAttachmentColumn())
                .parentColumn(session.getParentColumn())
                .epicColumn(session.getEpicColumn())
                .totalRows(session.getTotalRows())
                .validationResult(session.getValidationResult())
                .fieldMappings(session.getFieldMappings())
                .userMappings(session.getUserMappings())
                .optionMappings(session.getOptionMappings())
                .workflowStatusMappings(session.getWorkflowStatusMappings())
                .fieldDefaults(session.getFieldDefaults())
                .importOptions(session.getImportOptions())
                .sessionData(sessionData)
                .previewRows(session.getPreviewRows())
                .build();
    }
}
