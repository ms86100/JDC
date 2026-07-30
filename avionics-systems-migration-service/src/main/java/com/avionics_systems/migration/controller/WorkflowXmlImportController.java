package com.avionics_systems.migration.controller;

import com.avionics_systems.migration.dto.MigrationJobResponse;
import com.avionics_systems.migration.entity.MigrationJob;
import com.avionics_systems.migration.workflow.importing.WorkflowXmlImportJobProcessor;
import com.avionics_systems.migration.workflow.importing.WorkflowXmlImportService;
import com.avionics_systems.migration.workflow.importing.WorkflowXmlRollbackService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/migration/import/workflow-xml")
@RequiredArgsConstructor
@Tag(name = "Workflow XML Import", description = "Legacy DC workflow-descriptor XML import, validation, simulation, rollback")
public class WorkflowXmlImportController {

    private final WorkflowXmlImportService workflowXmlImportService;
    private final WorkflowXmlRollbackService workflowXmlRollbackService;
    private final WorkflowXmlImportJobProcessor workflowXmlImportJobProcessor;

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(
            @RequestParam("file") MultipartFile workflowFile,
            @RequestParam(value = "schemeFile", required = false) MultipartFile schemeFile) throws Exception {

        String workflowXml = new String(workflowFile.getBytes(), StandardCharsets.UTF_8);
        String schemeXml = schemeFile != null ? new String(schemeFile.getBytes(), StandardCharsets.UTF_8) : null;
        return ResponseEntity.ok(workflowXmlImportService.validateOnly(workflowXml, schemeXml));
    }

    @PostMapping
    public ResponseEntity<MigrationJobResponse> importWorkflow(
            @RequestParam("file") MultipartFile workflowFile,
            @RequestParam(value = "schemeFile", required = false) MultipartFile schemeFile,
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @RequestParam(value = "stubDownstream", defaultValue = "true") boolean stubDownstream,
            @RequestParam(value = "makeDefault", defaultValue = "false") boolean makeDefault,
            @RequestParam(value = "async", defaultValue = "true") boolean async,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) throws Exception {

        String workflowXml = new String(workflowFile.getBytes(), StandardCharsets.UTF_8);
        String schemeXml = schemeFile != null ? new String(schemeFile.getBytes(), StandardCharsets.UTF_8) : null;

        MigrationJob job = workflowXmlImportJobProcessor.createJob(projectId, userId, stubDownstream, makeDefault);
        if (async) {
            workflowXmlImportJobProcessor.processAsync(
                    job.getId(), workflowXml, schemeXml, projectId, userId, stubDownstream, makeDefault);
            return ResponseEntity.accepted().body(MigrationJobResponse.fromEntity(job));
        }
        workflowXmlImportJobProcessor.processAsync(
                job.getId(), workflowXml, schemeXml, projectId, userId, stubDownstream, makeDefault);
        return ResponseEntity.accepted().body(MigrationJobResponse.fromEntity(job));
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> importWorkflowSync(
            @RequestParam("file") MultipartFile workflowFile,
            @RequestParam(value = "schemeFile", required = false) MultipartFile schemeFile,
            @RequestParam(value = "jobId", required = false) UUID jobId,
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @RequestParam(value = "stubDownstream", defaultValue = "true") boolean stubDownstream,
            @RequestParam(value = "makeDefault", defaultValue = "false") boolean makeDefault,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) throws Exception {

        String workflowXml = new String(workflowFile.getBytes(), StandardCharsets.UTF_8);
        String schemeXml = schemeFile != null ? new String(schemeFile.getBytes(), StandardCharsets.UTF_8) : null;
        return ResponseEntity.ok(
                workflowXmlImportService.importWorkflow(workflowXml, schemeXml, jobId, projectId, userId,
                        stubDownstream, makeDefault));
    }

    @PostMapping("/simulate")
    public ResponseEntity<Map<String, Object>> simulate(
            @RequestParam("file") MultipartFile workflowFile,
            @RequestParam(value = "startStepId", defaultValue = "1") String startStepId,
            @RequestParam(value = "path", required = false) String transitionPath) throws Exception {

        return ResponseEntity.ok(workflowXmlImportService.simulateFromFile(
                new String(workflowFile.getBytes(), StandardCharsets.UTF_8), startStepId, transitionPath));
    }

    @PostMapping("/rollback/{importId}")
    public ResponseEntity<Map<String, Object>> rollback(
            @PathVariable UUID importId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(workflowXmlRollbackService.rollback(importId, userId));
    }

    @PostMapping("/validation-report")
    public ResponseEntity<String> validationReportCsv(
            @RequestParam("file") MultipartFile workflowFile,
            @RequestParam(value = "schemeFile", required = false) MultipartFile schemeFile) throws Exception {
        String workflowXml = new String(workflowFile.getBytes(), StandardCharsets.UTF_8);
        String schemeXml = schemeFile != null ? new String(schemeFile.getBytes(), StandardCharsets.UTF_8) : null;
        Map<String, Object> validation = workflowXmlImportService.validateOnly(workflowXml, schemeXml);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=workflow-validation-report.csv")
                .header("Content-Type", "text/csv")
                .body(toValidationCsv(validation));
    }

    private static String toValidationCsv(Map<String, Object> validation) {
        StringBuilder sb = new StringBuilder("severity,code,message\n");
        appendLines(sb, "ERROR", validation.get("errors"));
        appendLines(sb, "WARNING", validation.get("warnings"));
        @SuppressWarnings("unchecked")
        List<String> unsupported = (List<String>) validation.get("unsupportedFeatures");
        if (unsupported != null) {
            for (String u : unsupported) {
                sb.append("UNSUPPORTED,,\"").append(escape(u)).append("\"\n");
            }
        }
        return sb.toString();
    }

    private static void appendLines(StringBuilder sb, String severity, Object items) {
        if (!(items instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            sb.append(severity).append(",,\"").append(escape(String.valueOf(item))).append("\"\n");
        }
    }

    private static String escape(String s) {
        return s.replace("\"", "\"\"");
    }
}
