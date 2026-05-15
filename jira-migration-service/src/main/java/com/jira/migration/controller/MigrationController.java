package com.jira.migration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.migration.dto.*;
import com.jira.migration.entity.CsvTemplate;
import com.jira.migration.entity.FieldMapping;
import com.jira.migration.parser.CsvParser;
import com.jira.migration.parser.ValidationEngine;
import com.jira.migration.repository.CsvTemplateRepository;
import com.jira.migration.repository.FieldMappingRepository;
import com.jira.migration.service.MigrationService;
import com.jira.migration.async.ImportJobProcessor;
import com.jira.migration.async.ExportJobProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
@Slf4j
public class MigrationController {

    private final MigrationService migrationService;
    private final ImportJobProcessor importJobProcessor;
    private final ExportJobProcessor exportJobProcessor;
    private final CsvTemplateRepository csvTemplateRepository;
    private final FieldMappingRepository fieldMappingRepository;
    private final ValidationEngine validationEngine;
    private final CsvParser csvParser;
    private final ObjectMapper objectMapper;

    // ============================================
    // MIGRATION JOB MANAGEMENT
    // ============================================

    @PostMapping("/import/csv")
    public ResponseEntity<MigrationJobResponse> startCsvImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "templateId", required = false) UUID templateId,
            @RequestParam(value = "targetProjectId", required = false) UUID targetProjectId,
            @RequestParam(value = "options", required = false) String optionsJson,
            @RequestHeader("X-User-Id") UUID userId) {

        log.info("Starting CSV import: file={}, template={}, project={}",
                file.getOriginalFilename(), templateId, targetProjectId);

        StartMigrationRequest request = StartMigrationRequest.builder()
                .jobType("IMPORT")
                .importSource("CSV")
                .targetProjectId(targetProjectId)
                .templateId(templateId)
                .build();

        MigrationJobResponse job = migrationService.startImport(request, userId);

        // Start async processing
        importJobProcessor.processCsvImport(job.getId(), file, templateId,
                optionsJson != null ? parseJson(optionsJson) : Map.of(), userId);

        return ResponseEntity.accepted().body(job);
    }

    @PostMapping("/import/jira-dc")
    public ResponseEntity<MigrationJobResponse> startJiraDcImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "targetProjectId", required = false) UUID targetProjectId,
            @RequestParam(value = "options", required = false) String optionsJson,
            @RequestHeader("X-User-Id") UUID userId) {

        log.info("Starting Jira DC import: file={}", file.getOriginalFilename());

        StartMigrationRequest request = StartMigrationRequest.builder()
                .jobType("IMPORT")
                .importSource("JIRA_DC")
                .targetProjectId(targetProjectId)
                .build();

        MigrationJobResponse job = migrationService.startImport(request, userId);

        // Start async processing
        importJobProcessor.processJiraDcImport(job.getId(), file,
                optionsJson != null ? parseJson(optionsJson) : Map.of(), userId);

        return ResponseEntity.accepted().body(job);
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

    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<Void> cancelJob(
            @PathVariable UUID jobId,
            @RequestHeader("X-User-Id") UUID userId) {
        migrationService.cancelJob(jobId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<MigrationJobResponse>> listJobs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID userId) {
        // Implement filtering based on parameters
        return ResponseEntity.ok(List.of());
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