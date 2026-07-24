package com.jira.workflow.controller;

import com.jira.workflow.dto.*;
import com.jira.workflow.engine.script.GraalScriptEngine;
import com.jira.workflow.engine.script.ScriptExecutionService;
import com.jira.workflow.engine.script.ScriptResult;
import com.jira.workflow.entity.ScriptSchedule;
import com.jira.workflow.repository.ScriptScheduleRepository;
import com.jira.workflow.service.ScriptDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflow/scripts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Workflow Scripts", description = "CRUD and execution of JavaScript workflow scripts (JDC Script Engine)")
public class ScriptController {

    private final ScriptDefinitionService scriptDefinitionService;
    private final ScriptExecutionService scriptExecutionService;
    private final ScriptScheduleRepository scriptScheduleRepository;
    private final GraalScriptEngine graalScriptEngine;

    @GetMapping
    @Operation(summary = "List scripts", description = "List all scripts, optionally filtered by type")
    public ResponseEntity<List<ScriptResponse>> listScripts(
            @Parameter(description = "Filter by script type: CONDITION, VALIDATOR, POST_FUNCTION")
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(scriptDefinitionService.listScripts(type));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get script by ID")
    public ResponseEntity<ScriptResponse> getScript(
            @PathVariable UUID id) {
        return ResponseEntity.ok(scriptDefinitionService.getScript(id));
    }

    @PostMapping
    @Operation(summary = "Create a new script")
    public ResponseEntity<ScriptResponse> createScript(
            @Valid @RequestBody CreateScriptRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        UUID createdBy = userId != null ? UUID.fromString(userId) : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scriptDefinitionService.createScript(request, createdBy));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a script")
    public ResponseEntity<ScriptResponse> updateScript(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateScriptRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        UUID updatedBy = userId != null ? UUID.fromString(userId) : null;
        return ResponseEntity.ok(scriptDefinitionService.updateScript(id, request, updatedBy));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a script")
    public ResponseEntity<Void> deleteScript(@PathVariable UUID id) {
        scriptDefinitionService.deleteScript(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Enable or disable a script")
    public ResponseEntity<ScriptResponse> toggleScript(
            @PathVariable UUID id,
            @RequestParam boolean enabled,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        UUID updatedBy = userId != null ? UUID.fromString(userId) : null;
        return ResponseEntity.ok(scriptDefinitionService.toggleScript(id, enabled, updatedBy));
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Get version history for a script")
    public ResponseEntity<List<ScriptVersionResponse>> getVersions(
            @PathVariable UUID id) {
        return ResponseEntity.ok(scriptDefinitionService.getVersionHistory(id));
    }

    @PostMapping("/{id}/revert/{version}")
    @Operation(summary = "Revert script to a previous version")
    public ResponseEntity<ScriptResponse> revertToVersion(
            @PathVariable UUID id,
            @PathVariable Integer version,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        UUID updatedBy = userId != null ? UUID.fromString(userId) : null;
        return ResponseEntity.ok(scriptDefinitionService.revertToVersion(id, version, updatedBy));
    }

    @GetMapping("/{id}/executions")
    @Operation(summary = "Get execution log for a script")
    public ResponseEntity<Page<ScriptExecutionLogResponse>> getExecutionLog(
            @PathVariable UUID id, Pageable pageable) {
        return ResponseEntity.ok(scriptDefinitionService.getExecutionLogs(id, pageable));
    }

    @GetMapping("/executions")
    @Operation(summary = "Get all execution logs")
    public ResponseEntity<Page<ScriptExecutionLogResponse>> getAllExecutionLogs(Pageable pageable) {
        return ResponseEntity.ok(scriptDefinitionService.getAllExecutionLogs(pageable));
    }

    @PostMapping("/console")
    @Operation(summary = "Execute script in console mode for testing")
    public ResponseEntity<ScriptConsoleResponse> executeConsole(
            @Valid @RequestBody ScriptConsoleRequest request) {
        Map<String, Object> context = request.getContext() != null ? request.getContext() : Map.of();
        ScriptResult result = scriptExecutionService.executeConsole(
                request.getScriptBody(), request.getScriptType(), context);
        ScriptConsoleResponse response = ScriptConsoleResponse.builder()
                .success(result.success())
                .result(result.value())
                .errorMessage(result.errorMessage())
                .executionMs(result.executionMs())
                .consoleOutput(result.consoleOutput())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
    @Operation(summary = "List enabled scripts by type for workflow configuration dropdowns")
    public ResponseEntity<List<ScriptResponse>> getAvailableScripts(
            @Parameter(description = "Script type: CONDITION, VALIDATOR, POST_FUNCTION")
            @RequestParam String type) {
        return ResponseEntity.ok(scriptDefinitionService.listEnabledScriptsByType(type));
    }

    // === Validation ===

    @PostMapping("/validate")
    @Operation(summary = "Validate script syntax without executing")
    public ResponseEntity<Map<String, Object>> validateScript(
            @RequestBody Map<String, String> body) {
        String scriptBody = body.get("scriptBody");
        if (scriptBody == null || scriptBody.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "Script body is required"));
        }
        ScriptResult result = graalScriptEngine.execute(
                "\"use strict\";\n" + scriptBody, Map.of("jdc", Map.of(), "console", Map.of()), 2000);
        if (result.success()) {
            return ResponseEntity.ok(Map.of("valid", true));
        }
        return ResponseEntity.ok(Map.of("valid", false, "error", result.errorMessage()));
    }

    // === Import/Export ===

    @GetMapping("/{id}/export")
    @Operation(summary = "Export a script as JSON")
    public ResponseEntity<Map<String, Object>> exportScript(@PathVariable UUID id) {
        ScriptResponse script = scriptDefinitionService.getScript(id);
        Map<String, Object> export = new java.util.LinkedHashMap<>();
        export.put("name", script.getName());
        export.put("description", script.getDescription());
        export.put("scriptType", script.getScriptType());
        export.put("scriptKey", script.getScriptKey());
        export.put("scriptBody", script.getScriptBody());
        export.put("version", script.getVersion());
        return ResponseEntity.ok(export);
    }

    @PostMapping("/import")
    @Operation(summary = "Import a script from JSON")
    public ResponseEntity<ScriptResponse> importScript(
            @RequestBody CreateScriptRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        UUID createdBy = userId != null ? UUID.fromString(userId) : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scriptDefinitionService.createScript(request, createdBy));
    }

    // === Schedule Management ===

    @PostMapping("/{scriptId}/schedule")
    @Operation(summary = "Create a schedule for a script")
    public ResponseEntity<ScriptSchedule> createSchedule(
            @PathVariable UUID scriptId,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        String cron = body.get("cronExpression");
        if (cron == null || !CronExpression.isValidExpression(cron)) {
            return ResponseEntity.badRequest().build();
        }
        scriptDefinitionService.getScript(scriptId);
        CronExpression cronExpr = CronExpression.parse(cron);
        ScriptSchedule schedule = ScriptSchedule.builder()
                .scriptId(scriptId)
                .cronExpression(cron)
                .isEnabled(true)
                .nextRunAt(cronExpr.next(java.time.LocalDateTime.now()))
                .createdBy(userId != null ? UUID.fromString(userId) : null)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scriptScheduleRepository.save(schedule));
    }

    @GetMapping("/{scriptId}/schedule")
    @Operation(summary = "Get schedule for a script")
    public ResponseEntity<ScriptSchedule> getSchedule(@PathVariable UUID scriptId) {
        return scriptScheduleRepository.findByScriptId(scriptId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{scriptId}/schedule")
    @Operation(summary = "Delete schedule for a script")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID scriptId) {
        scriptScheduleRepository.findByScriptId(scriptId)
                .ifPresent(scriptScheduleRepository::delete);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{scriptId}/schedule/toggle")
    @Operation(summary = "Toggle schedule enabled state")
    public ResponseEntity<ScriptSchedule> toggleSchedule(@PathVariable UUID scriptId) {
        return scriptScheduleRepository.findByScriptId(scriptId)
                .map(schedule -> {
                    schedule.setIsEnabled(!schedule.getIsEnabled());
                    if (schedule.getIsEnabled()) {
                        CronExpression cron = CronExpression.parse(schedule.getCronExpression());
                        schedule.setNextRunAt(cron.next(java.time.LocalDateTime.now()));
                    }
                    return ResponseEntity.ok(scriptScheduleRepository.save(schedule));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
