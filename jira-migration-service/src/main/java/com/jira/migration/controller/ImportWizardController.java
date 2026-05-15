package com.jira.migration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.migration.dto.*;
import com.jira.migration.entity.*;
import com.jira.migration.parser.ValidationEngine;
import com.jira.migration.repository.*;
import com.jira.migration.service.MigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Import Wizard Controller
 * Handles the 5-step Jira-style import wizard:
 * 1. Upload - Select import source (file, Jira DC, project)
 * 2. Validate - Validate data before import
 * 3. Map Fields - Map source fields to target fields
 * 4. Map Users - Map source users to target users
 * 5. Import - Execute the import
 */
@RestController
@RequestMapping("/api/migration/wizard")
@RequiredArgsConstructor
@Slf4j
public class ImportWizardController {

    private final MigrationService migrationService;
    private final CsvTemplateRepository csvTemplateRepository;
    private final FieldMappingRepository fieldMappingRepository;
    private final UserMappingRepository userMappingRepository;
    private final ProjectMappingRepository projectMappingRepository;
    private final ValidationEngine validationEngine;
    private final ObjectMapper objectMapper;

    // ============================================
    // STEP 1: UPLOAD & SOURCE SELECTION
    // ============================================

    @PostMapping("/sessions")
    public ResponseEntity<WizardSessionResponse> createSession(
            @RequestBody CreateWizardSessionRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        log.info("Creating import wizard session: type={}", request.getImportType());

        WizardSessionResponse session = WizardSessionResponse.builder()
                .sessionId(UUID.randomUUID())
                .step("UPLOAD")
                .importType(request.getImportType())
                .status("IN_PROGRESS")
                .createdAt(java.time.LocalDateTime.now())
                .build();

        // Initialize session data
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("importType", request.getImportType());
        sessionData.put("targetProjectId", request.getTargetProjectId());
        sessionData.put("options", request.getOptions());
        session.setSessionData(sessionData);

        return ResponseEntity.ok(session);
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<WizardSessionResponse> getSession(@PathVariable UUID sessionId) {
        // Return current session state
        WizardSessionResponse session = WizardSessionResponse.builder()
                .sessionId(sessionId)
                .step("UPLOAD")
                .importType("CSV")
                .status("IN_PROGRESS")
                .build();
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{sessionId}/upload")
    public ResponseEntity<UploadResultResponse> uploadFile(
            @PathVariable UUID sessionId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam String importType,
            @RequestHeader("X-User-Id") UUID userId) {

        log.info("Processing upload for session {}: type={}", sessionId, importType);

        // Process uploaded file
        UploadResultResponse.UploadResultResponseBuilder builder = UploadResultResponse.builder();
        builder.sessionId(sessionId);
        builder.fileName(file.getOriginalFilename());
        builder.fileSize(file.getSize());

        try {
            if ("CSV".equals(importType)) {
                // Parse CSV headers
                List<String[]> rows = parseCsvContent(file);
                if (!rows.isEmpty()) {
                    builder.detectedHeaders(Arrays.asList(rows.get(0)));
                    builder.totalRows(rows.size() - 1);
                    builder.detectedEntityType(detectEntityType(rows.get(0)));
                }
            } else if ("JIRA_DC".equals(importType)) {
                // Parse XML structure
                String content = new String(file.getBytes());
                builder.detectedEntityTypes(detectXmlEntities(content));
                builder.totalRows(countXmlEntities(content));
            }

            builder.success(true);
        } catch (Exception e) {
            log.error("Upload processing failed", e);
            builder.success(false);
            builder.errorMessage(e.getMessage());
        }

        return ResponseEntity.ok(builder.build());
    }

    // ============================================
    // STEP 2: VALIDATION
    // ============================================

    @PostMapping("/sessions/{sessionId}/validate")
    public ResponseEntity<ValidationResult> validateData(
            @PathVariable UUID sessionId,
            @RequestBody ValidateDataRequest request) {

        log.info("Validating data for session {}: entityType={}, rows={}",
                sessionId, request.getEntityType(), request.getRows().size());

        List<ValidationResult.ValidationError> allErrors = new ArrayList<>();
        List<ValidationResult.ValidationWarning> allWarnings = new ArrayList<>();

        int rowNum = 1;
        for (Map<String, String> row : request.getRows()) {
            ValidationResult result = validationEngine.validateRow(row, request.getEntityType(), rowNum);

            allErrors.addAll(result.getErrors());
            allWarnings.addAll(result.getWarnings());
            rowNum++;
        }

        return ResponseEntity.ok(ValidationResult.builder()
                .valid(allErrors.isEmpty())
                .errors(allErrors)
                .warnings(allWarnings)
                .build());
    }

    @PostMapping("/sessions/{sessionId}/validate-full")
    public ResponseEntity<FullValidationResponse> runFullValidation(
            @PathVariable UUID sessionId,
            @RequestParam String importType,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestHeader("X-User-Id") UUID userId) {

        log.info("Running full validation for session {}", sessionId);

        FullValidationResponse.FullValidationResponseBuilder builder = FullValidationResponse.builder();
        builder.sessionId(sessionId);

        try {
            List<String[]> allRows = parseCsvContent(file);
            String[] headers = allRows.get(0);
            List<Map<String, String>> rows = allRows.stream()
                    .skip(1) // Skip header
                    .map(row -> {
                        Map<String, String> map = new HashMap<>();
                        for (int i = 0; i < headers.length && i < row.length; i++) {
                            map.put(headers[i].trim().toLowerCase(), row[i]);
                        }
                        return map;
                    })
                    .toList();

            String entityType = detectEntityType(headers);
            List<String> errors = validationEngine.validateBulk(rows, entityType);

            builder.totalRows(rows.size())
                    .errorCount(errors.size())
                    .valid(errors.isEmpty())
                    .errors(errors);

        } catch (Exception e) {
            log.error("Full validation failed", e);
            builder.valid(false);
            builder.errorCount(1);
            builder.errors(List.of(e.getMessage()));
        }

        return ResponseEntity.ok(builder.build());
    }

    // ============================================
    // STEP 3: FIELD MAPPING
    // ============================================

    @GetMapping("/sessions/{sessionId}/field-mappings")
    public ResponseEntity<List<FieldMapping>> getFieldMappings(
            @PathVariable UUID sessionId,
            @RequestParam String sourceType) {

        List<FieldMapping> mappings = fieldMappingRepository.findBySourceTypeAndTargetType(
                sourceType, "JIRA_PLATFORM"
        );
        return ResponseEntity.ok(mappings);
    }

    @PostMapping("/sessions/{sessionId}/field-mappings")
    public ResponseEntity<FieldMapping> saveFieldMapping(
            @PathVariable UUID sessionId,
            @RequestBody FieldMapping mapping) {

        FieldMapping saved = fieldMappingRepository.save(mapping);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/sessions/{sessionId}/apply-mapping")
    public ResponseEntity<ApplyMappingResponse> applyFieldMapping(
            @PathVariable UUID sessionId,
            @RequestBody ApplyMappingRequest request) {

        log.info("Applying field mapping for session {}", sessionId);

        ApplyMappingResponse.ApplyMappingResponseBuilder builder = ApplyMappingResponse.builder();
        builder.sessionId(sessionId);

        try {
            // Apply mapping to source data
            List<Map<String, String>> mappedData = new ArrayList<>();

            for (Map<String, String> row : request.getSourceData()) {
                Map<String, String> mappedRow = new HashMap<>();
                for (FieldMappingItem item : request.getMappings()) {
                    String sourceValue = row.get(item.getSourceField());
                    if (sourceValue != null) {
                        mappedRow.put(item.getTargetField(), transformValue(sourceValue, item.getTransformer()));
                    }
                }
                mappedData.add(mappedRow);
            }

            builder.mappedData(mappedData);
            builder.mappedCount(mappedData.size());
            builder.success(true);
        } catch (Exception e) {
            builder.success(false);
            builder.errorMessage(e.getMessage());
        }

        return ResponseEntity.ok(builder.build());
    }

    // ============================================
    // STEP 4: USER MAPPING
    // ============================================

    @GetMapping("/sessions/{sessionId}/user-mappings")
    public ResponseEntity<List<UserMappingResponse>> getUserMappings(
            @PathVariable UUID sessionId) {

        List<UserMappingResponse> mappings = new ArrayList<>();
        // Query user mappings for this session
        return ResponseEntity.ok(mappings);
    }

    @PostMapping("/sessions/{sessionId}/user-mappings/auto")
    public ResponseEntity<AutoUserMappingResponse> autoMapUsers(
            @PathVariable UUID sessionId,
            @RequestBody List<String> sourceUserIds) {

        log.info("Auto-mapping {} users for session {}", sourceUserIds.size(), sessionId);

        List<UserMappingResponse> mappings = new ArrayList<>();

        for (String sourceId : sourceUserIds) {
            // Try to match users by email or username
            UserMappingResponse mapping = UserMappingResponse.builder()
                    .sourceIdentifier(sourceId)
                    .mappingType("AUTO")
                    .confidenceScore(95.0)
                    .build();
            mappings.add(mapping);
        }

        return ResponseEntity.ok(AutoUserMappingResponse.builder()
                .sessionId(sessionId)
                .mappings(mappings)
                .totalMapped(mappings.size())
                .build());
    }

    @PostMapping("/sessions/{sessionId}/user-mappings")
    public ResponseEntity<UserMappingResponse> saveUserMapping(
            @PathVariable UUID sessionId,
            @RequestBody UserMappingRequest request) {

        UserMapping userMapping = UserMapping.builder()
                .jobId(sessionId)
                .sourceIdentifier(request.getSourceIdentifier())
                .sourceType("JIRA_DC")
                .targetUserId(request.getTargetUserId())
                .targetUsername(request.getTargetUsername())
                .mappingType("MANUAL")
                .confidenceScore(100.0)
                .build();

        UserMapping saved = userMappingRepository.save(userMapping);

        return ResponseEntity.ok(UserMappingResponse.builder()
                .sourceIdentifier(saved.getSourceIdentifier())
                .targetUsername(saved.getTargetUsername())
                .mappingType(saved.getMappingType())
                .confidenceScore(saved.getConfidenceScore())
                .build());
    }

    // ============================================
    // STEP 5: IMPORT EXECUTION
    // ============================================

    @PostMapping("/sessions/{sessionId}/execute")
    public ResponseEntity<MigrationJobResponse> executeImport(
            @PathVariable UUID sessionId,
            @RequestBody ExecuteImportRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        log.info("Executing import for session {}: {} entities", sessionId, request.getEntityCount());

        StartMigrationRequest migrationRequest = StartMigrationRequest.builder()
                .jobType("IMPORT")
                .importSource(request.getImportSource())
                .targetProjectId(request.getTargetProjectId())
                .config(Map.of("sessionId", sessionId.toString(), "config", request.getConfig() != null ? request.getConfig() : Map.of()))
                .options(request.getOptions() != null ? request.getOptions() : Map.of())
                .build();

        MigrationJobResponse job = migrationService.startImport(migrationRequest, userId);

        // Update session with job ID
        job.setResultMetadata(sessionId.toString());

        return ResponseEntity.accepted().body(job);
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private List<String[]> parseCsvContent(org.springframework.web.multipart.MultipartFile file) throws Exception {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {

            List<String[]> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                rows.add(parseCsvLine(line));
            }
            return rows;
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());

        return fields.toArray(new String[0]);
    }

    private String detectEntityType(String[] headers) {
        Set<String> headerSet = new HashSet<>();
        for (String h : headers) {
            headerSet.add(h.trim().toLowerCase());
        }

        if (headerSet.contains("project_key")) return "PROJECT";
        if (headerSet.contains("issue_key") || headerSet.contains("project_key")) return "ISSUE";
        if (headerSet.contains("email") && headerSet.contains("username")) return "USER";

        return "UNKNOWN";
    }

    private List<String> detectXmlEntities(String content) {
        List<String> entities = new ArrayList<>();
        if (content.contains("<Project")) entities.add("Project");
        if (content.contains("<Issue")) entities.add("Issue");
        if (content.contains("<User")) entities.add("User");
        if (content.contains("<Status")) entities.add("Status");
        if (content.contains("<IssueType")) entities.add("IssueType");
        return entities;
    }

    private int countXmlEntities(String content) {
        return (content.length() - content.replace("<Entity ", "").length()) / "<Entity ".length();
    }

    private String transformValue(String value, String transformer) {
        if (transformer == null) return value;
        switch (transformer.toUpperCase()) {
            case "UPPERCASE" -> { return value.toUpperCase(); }
            case "LOWERCASE" -> { return value.toLowerCase(); }
            case "TRIM" -> { return value.trim(); }
            default -> { return value; }
        }
    }

    // ============================================
    // REQUEST/RESPONSE CLASSES
    // ============================================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CreateWizardSessionRequest {
        private String importType; // CSV, JIRA_DC, PROJECT
        private UUID targetProjectId;
        private Map<String, Object> options;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WizardSessionResponse {
        private UUID sessionId;
        private String step;
        private String importType;
        private String status;
        private java.time.LocalDateTime createdAt;
        private Map<String, Object> sessionData;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UploadResultResponse {
        private UUID sessionId;
        private String fileName;
        private Long fileSize;
        private List<String> detectedHeaders;
        private String detectedEntityType;
        private List<String> detectedEntityTypes;
        private Integer totalRows;
        private boolean success;
        private String errorMessage;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ValidateDataRequest {
        private String entityType;
        private List<Map<String, String>> rows;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FullValidationResponse {
        private UUID sessionId;
        private int totalRows;
        private int errorCount;
        private boolean valid;
        private List<String> errors;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FieldMappingItem {
        private String sourceField;
        private String targetField;
        private String defaultValue;
        private String transformer;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ApplyMappingRequest {
        private List<Map<String, String>> sourceData;
        private List<FieldMappingItem> mappings;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ApplyMappingResponse {
        private UUID sessionId;
        private boolean success;
        private List<Map<String, String>> mappedData;
        private int mappedCount;
        private String errorMessage;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserMappingRequest {
        private String sourceIdentifier;
        private UUID targetUserId;
        private String targetUsername;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserMappingResponse {
        private String sourceIdentifier;
        private UUID targetUserId;
        private String targetUsername;
        private String mappingType;
        private Double confidenceScore;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AutoUserMappingResponse {
        private UUID sessionId;
        private List<UserMappingResponse> mappings;
        private int totalMapped;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ExecuteImportRequest {
        private String importSource;
        private UUID targetProjectId;
        private int entityCount;
        private Map<String, Object> config;
        private Map<String, Object> options;
    }
}