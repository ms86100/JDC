package com.jira.migration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.migration.async.ImportJobProcessor;
import com.jira.migration.dc.JiraDcImportApiService;
import com.jira.migration.dto.MigrationJobResponse;
import com.jira.migration.dto.StartMigrationRequest;
import com.jira.migration.dto.ValidationResult;
import com.jira.migration.dto.wizard.*;
import com.jira.migration.entity.MigrationFileUpload;
import com.jira.migration.entity.WizardSession;
import com.jira.migration.exception.EntityNotFoundException;
import com.jira.migration.parser.CsvParser;
import com.jira.migration.parser.ImportSpreadsheetParser;
import com.jira.migration.parser.ValidationEngine;
import com.jira.migration.repository.MigrationFileUploadRepository;
import com.jira.migration.repository.MigrationJobRepository;
import com.jira.migration.repository.WizardSessionRepository;
import com.jira.migration.entity.MigrationJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private static final int PREVIEW_ROW_LIMIT = 25;
    private static final int SESSION_TTL_HOURS = 24;

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
    private final JiraDcImportApiService jiraDcImportApiService;

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
                .expiresAt(LocalDateTime.now().plusHours(SESSION_TTL_HOURS))
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
            if ("JIRA_DC".equals(importType) || "ISSUE_XML".equals(importType)
                    || (fileName != null && fileName.toLowerCase().endsWith(".xml"))) {
                parseJiraDcIntoSession(session, content, result);
            } else {
                parseSpreadsheetIntoSession(session, content, fileName, result);
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
        if ("JIRA_DC".equals(session.getImportType()) || "ISSUE_XML".equals(session.getImportType())) {
            return validateJiraDcSession(sessionId, session);
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
            for (int i = 0; i < Math.min(parsed.getDataRows().size(), PREVIEW_ROW_LIMIT); i++) {
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
                && !"JIRA_DC".equals(session.getImportType())) {
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

        byte[] fileContent = upload.getFileContent();
        String fileName = upload.getFileName();

        if ("JIRA_DC".equals(session.getImportType()) || "ISSUE_XML".equals(session.getImportType())) {
            importJobProcessor.processJiraDcImport(job.getId(), fileContent, fileName, options, userId);
        } else {
            importJobProcessor.processSpreadsheetImport(job.getId(), fileContent, fileName, null, options, userId);
        }

        log.info("Wizard session {} started migration job {}", sessionId, job.getId());
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
        for (int i = 0; i < Math.min(parsed.getDataRows().size(), PREVIEW_ROW_LIMIT); i++) {
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

    private void parseJiraDcIntoSession(WizardSession session, byte[] content,
                                        WizardUploadResultDto.WizardUploadResultDtoBuilder result) {
        String xml = new String(content, StandardCharsets.UTF_8);
        List<String> entities = new ArrayList<>();
        if (xml.contains("<Project")) entities.add("Project");
        if (xml.contains("<Issue")) entities.add("Issue");
        if (xml.contains("<User")) entities.add("User");
        session.setDetectedEntityType("ISSUE");
        session.setDetectedHeaders(entities);
        result.detectedEntityTypes(entities).detectedEntityType("JIRA_DC");
    }

    private ValidationResult validateJiraDcSession(UUID sessionId, WizardSession session) {
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
            Map<String, Object> apiResult = jiraDcImportApiService.validateUpload(
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
                    "JIRA_DC",
                    result,
                    true);

            session.setValidationResult(Map.of(
                    "valid", valid,
                    "errorCount", errors.size(),
                    "warningCount", apiResult.getOrDefault("warningCount", 0),
                    "totalRows", apiResult.getOrDefault("totalEntities", 0),
                    "entityType", "JIRA_DC",
                    "format", apiResult.get("format"),
                    "riskScore", apiResult.get("riskScore")
            ));
            session.advanceStep("MAP_FIELDS");
            wizardSessionRepository.save(session);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Jira DC validation failed: " + e.getMessage(), e);
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
            rows.add(map);
        }
        return rows;
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
        long maxBytes = 500L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File exceeds maximum size of 500MB");
        }
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        boolean csv = name.endsWith(".csv");
        boolean xml = name.endsWith(".xml");
        boolean xlsx = name.endsWith(".xlsx");
        boolean zip = name.endsWith(".zip");
        if (!csv && !xml && !xlsx && !zip) {
            throw new IllegalArgumentException("Supported formats: CSV, XLSX (Excel), XML/ZIP (Jira DC)");
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
        if (type == null) return "CSV";
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "csv" -> "CSV";
            case "issue-xml", "issue_xml", "issuexml" -> "ISSUE_XML";
            case "jira-dc", "jira_dc", "jiradc" -> "JIRA_DC";
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
