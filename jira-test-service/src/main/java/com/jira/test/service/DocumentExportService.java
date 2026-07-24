package com.jira.test.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.test.dto.CreateExportTemplateRequest;
import com.jira.test.dto.ExportTemplateResponse;
import com.jira.test.entity.*;
import com.jira.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentExportService {

    private final ExportTemplateRepository templateRepo;
    private final VvoDefinitionRepository vvoRepo;
    private final TechEventRepository techEventRepo;
    private final BenchDefectRepository benchDefectRepo;
    private final ProblemReportRepository problemReportRepo;
    private final TestExecutionRepository testExecutionRepo;
    private final RequirementLinkRepository requirementLinkRepo;
    private final DefectLinkRepository defectLinkRepo;
    private final ObjectMapper objectMapper;

    // ── Template CRUD ──────────────────────────────────────────────────────

    @Transactional
    public ExportTemplateResponse createTemplate(CreateExportTemplateRequest request) {
        if (templateRepo.existsByName(request.getName())) {
            throw new RuntimeException("Template with name already exists: " + request.getName());
        }

        ExportTemplate template = ExportTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .templateType(request.getTemplateType() != null ? request.getTemplateType() : "CSV")
                .outputFormat(request.getOutputFormat() != null ? request.getOutputFormat() : "CSV")
                .sourceType(request.getSourceType())
                .columns(serializeColumns(request.getColumns()))
                .groupBy(request.getGroupBy())
                .sortBy(request.getSortBy())
                .sortDirection(request.getSortDirection() != null ? request.getSortDirection() : "ASC")
                .headerText(request.getHeaderText())
                .footerText(request.getFooterText())
                .filterJql(request.getFilterJql())
                .isSystem(false)
                .build();

        template = templateRepo.save(template);
        log.info("Created export template: {} (source={})", template.getName(), template.getSourceType());
        return toResponse(template);
    }

    @Transactional(readOnly = true)
    public ExportTemplateResponse getTemplate(UUID id) {
        ExportTemplate template = templateRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));
        return toResponse(template);
    }

    @Transactional(readOnly = true)
    public List<ExportTemplateResponse> getTemplatesBySourceType(String sourceType) {
        return templateRepo.findBySourceTypeOrderByNameAsc(sourceType).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExportTemplateResponse> getSystemTemplates() {
        return templateRepo.findByIsSystemTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ExportTemplateResponse updateTemplate(UUID id, CreateExportTemplateRequest request) {
        ExportTemplate template = templateRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));

        if (Boolean.TRUE.equals(template.getIsSystem())) {
            throw new RuntimeException("Cannot modify system template: " + template.getName());
        }

        // Check name uniqueness if name is changing
        if (!template.getName().equals(request.getName()) && templateRepo.existsByName(request.getName())) {
            throw new RuntimeException("Template with name already exists: " + request.getName());
        }

        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setTemplateType(request.getTemplateType() != null ? request.getTemplateType() : template.getTemplateType());
        template.setOutputFormat(request.getOutputFormat() != null ? request.getOutputFormat() : template.getOutputFormat());
        template.setSourceType(request.getSourceType());
        template.setColumns(serializeColumns(request.getColumns()));
        template.setGroupBy(request.getGroupBy());
        template.setSortBy(request.getSortBy());
        template.setSortDirection(request.getSortDirection() != null ? request.getSortDirection() : "ASC");
        template.setHeaderText(request.getHeaderText());
        template.setFooterText(request.getFooterText());
        template.setFilterJql(request.getFilterJql());

        template = templateRepo.save(template);
        log.info("Updated export template: {}", template.getName());
        return toResponse(template);
    }

    @Transactional
    public void deleteTemplate(UUID id) {
        ExportTemplate template = templateRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));

        if (Boolean.TRUE.equals(template.getIsSystem())) {
            throw new RuntimeException("Cannot delete system template: " + template.getName());
        }

        templateRepo.delete(template);
        log.info("Deleted export template: {}", template.getName());
    }

    // ── Document Generation ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String generateDocument(UUID templateId, UUID projectId, UUID fixVersionId, UUID testPlanId) {
        ExportTemplate template = templateRepo.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        // 1. Fetch data based on sourceType
        List<Map<String, Object>> rows = fetchData(template, projectId, fixVersionId, testPlanId);

        // 2. Sort
        if (template.getSortBy() != null) {
            rows.sort(Comparator.comparing(r -> String.valueOf(r.getOrDefault(template.getSortBy(), ""))));
            if ("DESC".equalsIgnoreCase(template.getSortDirection())) {
                Collections.reverse(rows);
            }
        }

        // 3. Generate output based on format
        String output = switch (template.getOutputFormat().toUpperCase()) {
            case "CSV" -> generateCsv(template, rows);
            default -> generateCsv(template, rows);
        };

        log.info("Generated document from template '{}': {} rows, format={}",
                template.getName(), rows.size(), template.getOutputFormat());
        return output;
    }

    // ── Data Fetchers ──────────────────────────────────────────────────────

    private List<Map<String, Object>> fetchData(ExportTemplate template, UUID projectId, UUID fixVersionId, UUID testPlanId) {
        return switch (template.getSourceType().toUpperCase()) {
            case "VVO" -> fetchVvoData(projectId, fixVersionId);
            case "TECH_EVENT" -> fetchTechEventData(projectId);
            case "BENCH_DEFECT" -> fetchBenchDefectData(projectId);
            case "PROBLEM_REPORT" -> fetchProblemReportData(projectId);
            case "TEST_EXECUTION" -> fetchTestExecutionData(testPlanId);
            default -> List.of();
        };
    }

    private List<Map<String, Object>> fetchVvoData(UUID projectId, UUID fixVersionId) {
        List<VvoDefinition> vvos;
        if (fixVersionId != null) {
            vvos = vvoRepo.findByFixVersionId(fixVersionId).stream()
                    .filter(v -> projectId == null || projectId.equals(v.getProjectId()))
                    .toList();
        } else if (projectId != null) {
            vvos = vvoRepo.findByProjectIdAndArchivedFalse(projectId);
        } else {
            return List.of();
        }

        return vvos.stream().map(vvo -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("issueKey", vvo.getIssueKey());
            row.put("summary", vvo.getSummary());
            row.put("status", vvo.getStatus());
            row.put("vvoVersion", vvo.getVvoVersion());
            row.put("idDoors", vvo.getIdDoors());
            row.put("applicability", joinList(vvo.getApplicability()));
            row.put("supplierApplicability", joinList(vvo.getSupplierApplicability()));
            row.put("operationalConditions", vvo.getOperationalConditions());
            row.put("expectedResults", vvo.getExpectedResults());
            row.put("component", vvo.getComponentIds() != null ? vvo.getComponentIds().toString() : "");

            // Find linked tests via RequirementLink
            List<RequirementLink> links = vvo.getIssueKey() != null
                    ? requirementLinkRepo.findByRequirementKey(vvo.getIssueKey())
                    : List.of();
            row.put("linkedTests", links.stream()
                    .map(l -> l.getTestId().toString())
                    .collect(Collectors.joining("; ")));
            row.put("linkedTestCount", links.size());
            row.put("coverageStatus", links.isEmpty() ? "NOT_COVERED" : "COVERED");

            // Find linked defects via DefectLink on executions
            List<String> defectKeys = links.stream()
                    .flatMap(link -> defectLinkRepo.findByExecutionId(link.getTestId()).stream())
                    .map(DefectLink::getDefectKey)
                    .distinct()
                    .toList();
            row.put("linkedDefects", String.join("; ", defectKeys));

            // Test execution status summary
            row.put("testExecution", links.isEmpty() ? "NO_TEST" : "LINKED");

            return row;
        }).toList();
    }

    private List<Map<String, Object>> fetchTechEventData(UUID projectId) {
        if (projectId == null) {
            return List.of();
        }

        List<TechEvent> events = techEventRepo.findByProjectIdOrderByCreatedAtDesc(projectId);

        return events.stream().map(te -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("issueKey", te.getIssueKey());
            row.put("summary", te.getSummary());
            row.put("status", te.getStatus());
            row.put("defectType", te.getDefectType());
            row.put("defectOrigin", te.getDefectOrigin());
            row.put("defectImpact", te.getDefectImpact());
            row.put("priority", te.getPriority());
            row.put("detectedOnProgramId", te.getDetectedOnProgramId());
            row.put("systemSupplierId", te.getSystemSupplierId());
            row.put("vvActivity", te.getVvActivity());
            row.put("detectedBy", te.getDetectedBy());
            return row;
        }).toList();
    }

    private List<Map<String, Object>> fetchBenchDefectData(UUID projectId) {
        if (projectId == null) {
            return List.of();
        }

        List<BenchDefect> defects = benchDefectRepo.findByProjectIdOrderByCreatedAtDesc(projectId);

        return defects.stream().map(bd -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("issueKey", bd.getIssueKey());
            row.put("summary", bd.getSummary());
            row.put("status", bd.getStatus());
            row.put("severity", bd.getSeverity());
            row.put("criticality", bd.getCriticality());
            row.put("defectType", bd.getDefectType());
            row.put("defectOrigin", bd.getDefectOrigin());
            row.put("defectImpact", bd.getDefectImpact());
            row.put("priority", bd.getPriority());
            row.put("workaround", bd.getWorkaround());
            row.put("changeReference", bd.getChangeReference());
            return row;
        }).toList();
    }

    private List<Map<String, Object>> fetchProblemReportData(UUID projectId) {
        if (projectId == null) {
            return List.of();
        }

        List<ProblemReport> reports = problemReportRepo.findByProjectIdOrderByCreatedAtDesc(projectId);

        return reports.stream().map(pr -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("issueKey", pr.getIssueKey());
            row.put("summary", pr.getSummary());
            row.put("status", pr.getStatus());
            row.put("prOrigin", pr.getPrOrigin());
            row.put("prType", pr.getPrType());
            row.put("classification", pr.getClassification());
            row.put("priority", pr.getPriority());
            row.put("potentialEffects", pr.getPotentialEffects());
            row.put("justificationMitigation", pr.getJustificationMitigation());
            row.put("systemSupplierId", pr.getSystemSupplierId());
            return row;
        }).toList();
    }

    private List<Map<String, Object>> fetchTestExecutionData(UUID testPlanId) {
        if (testPlanId == null) {
            return List.of();
        }

        List<TestExecution> executions = testExecutionRepo.findByTestPlanId(testPlanId);

        return executions.stream().map(te -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("testKey", te.getTestId());
            row.put("testName", te.getName());
            row.put("executionStatus", te.getStatus());
            row.put("testEnv", te.getTestEnv());
            row.put("startedAt", te.getStartedAt());
            row.put("finishedAt", te.getFinishedAt());
            row.put("component", "");

            // Find linked defects
            List<DefectLink> defects = defectLinkRepo.findByExecutionId(te.getId());
            row.put("defects", defects.stream()
                    .map(DefectLink::getDefectKey)
                    .collect(Collectors.joining("; ")));

            return row;
        }).toList();
    }

    // ── CSV Generation ─────────────────────────────────────────────────────

    private String generateCsv(ExportTemplate template, List<Map<String, Object>> rows) {
        List<Map<String, String>> cols = parseColumns(template.getColumns());
        if (cols.isEmpty()) {
            return "";
        }

        StringBuilder csv = new StringBuilder();

        // Header text
        if (template.getHeaderText() != null) {
            csv.append(template.getHeaderText()).append("\n");
        }

        // Column headers
        csv.append(cols.stream()
                .map(c -> escapeCsv(c.get("header")))
                .collect(Collectors.joining(","))).append("\n");

        // Group if configured
        if (template.getGroupBy() != null) {
            Map<String, List<Map<String, Object>>> grouped = rows.stream()
                    .collect(Collectors.groupingBy(
                            r -> String.valueOf(r.getOrDefault(template.getGroupBy(), "Ungrouped")),
                            LinkedHashMap::new,
                            Collectors.toList()));

            for (Map.Entry<String, List<Map<String, Object>>> group : grouped.entrySet()) {
                csv.append("\n[CLUSTER] ").append(group.getKey()).append("\n");
                for (Map<String, Object> row : group.getValue()) {
                    csv.append(formatCsvRow(cols, row)).append("\n");
                }
            }
        } else {
            for (Map<String, Object> row : rows) {
                csv.append(formatCsvRow(cols, row)).append("\n");
            }
        }

        // Footer text
        if (template.getFooterText() != null) {
            csv.append("\n").append(template.getFooterText());
        }

        return csv.toString();
    }

    private String formatCsvRow(List<Map<String, String>> cols, Map<String, Object> row) {
        return cols.stream()
                .map(c -> escapeCsv(String.valueOf(row.getOrDefault(c.get("key"), ""))))
                .collect(Collectors.joining(","));
    }

    private String escapeCsv(String v) {
        if (v == null || "null".equals(v)) {
            return "";
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private List<Map<String, String>> parseColumns(String columnsJson) {
        if (columnsJson == null || columnsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(columnsJson, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse columns JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private String serializeColumns(List<Map<String, String>> columns) {
        if (columns == null || columns.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(columns);
        } catch (Exception e) {
            log.warn("Failed to serialize columns: {}", e.getMessage());
            return "[]";
        }
    }

    private String joinList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join("; ", list);
    }

    private ExportTemplateResponse toResponse(ExportTemplate template) {
        return ExportTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .templateType(template.getTemplateType())
                .outputFormat(template.getOutputFormat())
                .sourceType(template.getSourceType())
                .columns(parseColumns(template.getColumns()))
                .groupBy(template.getGroupBy())
                .sortBy(template.getSortBy())
                .sortDirection(template.getSortDirection())
                .headerText(template.getHeaderText())
                .footerText(template.getFooterText())
                .filterJql(template.getFilterJql())
                .isSystem(template.getIsSystem())
                .createdBy(template.getCreatedBy())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
