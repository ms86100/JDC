package com.avionics_systems.workflow.controller;

import com.avionics_systems.workflow.dto.*;
import com.avionics_systems.workflow.engine.script.GraalScriptEngine;
import com.avionics_systems.workflow.engine.script.JdcDslTranspiler;
import com.avionics_systems.workflow.engine.script.ScriptExecutionService;
import com.avionics_systems.workflow.engine.script.ScriptResult;
import com.avionics_systems.workflow.entity.ScriptSchedule;
import com.avionics_systems.workflow.repository.ScriptScheduleRepository;
import com.avionics_systems.workflow.service.ScriptDefinitionService;
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
    private final com.avionics_systems.workflow.service.ScriptListenerService scriptListenerService;
    private final com.avionics_systems.workflow.service.ScriptFieldBehaviorService scriptFieldBehaviorService;
    private final com.avionics_systems.workflow.service.ScriptCalculatedFieldService scriptCalculatedFieldService;

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

    // === Dashboard / Metrics ===

    @GetMapping("/dashboard")
    @Operation(summary = "Script engine dashboard metrics")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        List<ScriptResponse> all = scriptDefinitionService.listScripts(null);
        long total = all.size();
        long enabled = all.stream().filter(ScriptResponse::getIsEnabled).count();
        long conditions = all.stream().filter(s -> "CONDITION".equals(s.getScriptType())).count();
        long validators = all.stream().filter(s -> "VALIDATOR".equals(s.getScriptType())).count();
        long postFunctions = all.stream().filter(s -> "POST_FUNCTION".equals(s.getScriptType())).count();

        Map<String, Object> dashboard = new java.util.LinkedHashMap<>();
        dashboard.put("totalScripts", total);
        dashboard.put("enabledScripts", enabled);
        dashboard.put("disabledScripts", total - enabled);
        dashboard.put("conditions", conditions);
        dashboard.put("validators", validators);
        dashboard.put("postFunctions", postFunctions);
        return ResponseEntity.ok(dashboard);
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
        try {
            String transpiled = JdcDslTranspiler.transpile(scriptBody);
            graalScriptEngine.parseOnly(transpiled);
            return ResponseEntity.ok(Map.of("valid", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("valid", false, "error", e.getMessage()));
        }
    }

    // === DSL Transpilation Preview ===

    @PostMapping("/transpile")
    @Operation(summary = "Preview DSL-to-JavaScript transpilation output")
    public ResponseEntity<Map<String, Object>> transpileScript(@RequestBody Map<String, String> body) {
        String scriptBody = body.get("scriptBody");
        if (scriptBody == null || scriptBody.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Script body is required"));
        }
        String transpiled = JdcDslTranspiler.transpile(scriptBody);
        boolean hasDsl = JdcDslTranspiler.containsDslSyntax(scriptBody);
        return ResponseEntity.ok(Map.of(
            "original", scriptBody,
            "transpiled", transpiled,
            "hasDslSyntax", hasDsl,
            "changed", !scriptBody.equals(transpiled)));
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

    // === Event Listeners ===

    @PostMapping("/{scriptId}/listeners")
    @Operation(summary = "Add event listener for a script")
    public ResponseEntity<com.avionics_systems.workflow.entity.ScriptListener> createListener(
            @PathVariable UUID scriptId,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        scriptDefinitionService.getScript(scriptId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                scriptListenerService.createListener(scriptId,
                        body.get("eventType"),
                        body.get("projectFilter") != null ? UUID.fromString(body.get("projectFilter")) : null,
                        body.get("issueTypeFilter") != null ? UUID.fromString(body.get("issueTypeFilter")) : null,
                        userId != null ? UUID.fromString(userId) : null));
    }

    @GetMapping("/{scriptId}/listeners")
    @Operation(summary = "Get listeners for a script")
    public ResponseEntity<List<com.avionics_systems.workflow.entity.ScriptListener>> getListeners(@PathVariable UUID scriptId) {
        return ResponseEntity.ok(scriptListenerService.getListenersForScript(scriptId));
    }

    @GetMapping("/listeners")
    @Operation(summary = "Get all listeners across all scripts")
    public ResponseEntity<List<com.avionics_systems.workflow.entity.ScriptListener>> getAllListeners() {
        return ResponseEntity.ok(scriptListenerService.getAllListeners());
    }

    @DeleteMapping("/listeners/{listenerId}")
    @Operation(summary = "Delete a listener")
    public ResponseEntity<Void> deleteListener(@PathVariable UUID listenerId) {
        scriptListenerService.deleteListener(listenerId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/listeners/{listenerId}/toggle")
    @Operation(summary = "Toggle a listener's enabled state")
    public ResponseEntity<com.avionics_systems.workflow.entity.ScriptListener> toggleListener(@PathVariable UUID listenerId) {
        return ResponseEntity.ok(scriptListenerService.toggleListener(listenerId));
    }

    // === Field Behaviors ===

    @PostMapping("/field-behaviors/evaluate")
    @Operation(summary = "Evaluate field behavior scripts for a screen context")
    public ResponseEntity<Map<String, Object>> evaluateFieldBehaviors(@RequestBody Map<String, Object> body) {
        String screenContext = (String) body.getOrDefault("screenContext", "EDIT");
        UUID projectId = body.get("projectId") != null ? UUID.fromString(body.get("projectId").toString()) : null;
        UUID issueTypeId = body.get("issueTypeId") != null ? UUID.fromString(body.get("issueTypeId").toString()) : null;
        UUID userId = body.get("userId") != null ? UUID.fromString(body.get("userId").toString()) : null;
        @SuppressWarnings("unchecked")
        Map<String, Object> issueData = (Map<String, Object>) body.getOrDefault("issueData", Map.of());

        List<Map<String, Object>> fields = scriptFieldBehaviorService.evaluateFieldBehaviors(
                screenContext, projectId, issueTypeId, issueData, userId);
        return ResponseEntity.ok(Map.of("fields", fields));
    }

    @PostMapping("/{scriptId}/field-behaviors")
    @Operation(summary = "Create a field behavior for a script")
    public ResponseEntity<com.avionics_systems.workflow.entity.ScriptFieldBehavior> createFieldBehavior(
            @PathVariable UUID scriptId,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        scriptDefinitionService.getScript(scriptId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                scriptFieldBehaviorService.createBehavior(scriptId,
                        body.getOrDefault("screenContext", "EDIT"),
                        body.get("projectId") != null ? UUID.fromString(body.get("projectId")) : null,
                        body.get("issueTypeId") != null ? UUID.fromString(body.get("issueTypeId")) : null,
                        userId != null ? UUID.fromString(userId) : null));
    }

    @GetMapping("/{scriptId}/field-behaviors")
    @Operation(summary = "Get field behaviors for a script")
    public ResponseEntity<List<com.avionics_systems.workflow.entity.ScriptFieldBehavior>> getFieldBehaviors(@PathVariable UUID scriptId) {
        return ResponseEntity.ok(scriptFieldBehaviorService.getBehaviorsForScript(scriptId));
    }

    @GetMapping("/field-behaviors")
    @Operation(summary = "Get all field behaviors across all scripts")
    public ResponseEntity<List<com.avionics_systems.workflow.entity.ScriptFieldBehavior>> getAllBehaviors() {
        return ResponseEntity.ok(scriptFieldBehaviorService.getAllBehaviors());
    }

    @DeleteMapping("/field-behaviors/{behaviorId}")
    @Operation(summary = "Delete a field behavior")
    public ResponseEntity<Void> deleteFieldBehavior(@PathVariable UUID behaviorId) {
        scriptFieldBehaviorService.deleteBehavior(behaviorId);
        return ResponseEntity.noContent().build();
    }

    // === Calculated Fields ===

    @GetMapping("/calculated-fields/evaluate")
    @Operation(summary = "Evaluate a calculated field script for an issue")
    public ResponseEntity<Map<String, Object>> evaluateCalculatedField(
            @RequestParam UUID issueId,
            @RequestParam UUID fieldId) {
        return ResponseEntity.ok(scriptCalculatedFieldService.evaluateField(issueId, fieldId));
    }

    @PostMapping("/{scriptId}/calculated-fields")
    @Operation(summary = "Bind a script to a custom field as a calculated field")
    public ResponseEntity<com.avionics_systems.workflow.entity.ScriptCalculatedField> createCalculatedField(
            @PathVariable UUID scriptId,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        scriptDefinitionService.getScript(scriptId);
        Long ttl = body.get("cacheTtlMs") != null ? Long.parseLong(body.get("cacheTtlMs")) : 0L;
        return ResponseEntity.status(HttpStatus.CREATED).body(
                scriptCalculatedFieldService.createBinding(scriptId,
                        UUID.fromString(body.get("customFieldId")), ttl,
                        userId != null ? UUID.fromString(userId) : null));
    }

    @GetMapping("/{scriptId}/calculated-fields")
    @Operation(summary = "Get calculated field bindings for a script")
    public ResponseEntity<List<com.avionics_systems.workflow.entity.ScriptCalculatedField>> getCalculatedFields(@PathVariable UUID scriptId) {
        return ResponseEntity.ok(scriptCalculatedFieldService.getBindingsForScript(scriptId));
    }

    @DeleteMapping("/calculated-fields/{bindingId}")
    @Operation(summary = "Remove a calculated field binding")
    public ResponseEntity<Void> deleteCalculatedField(@PathVariable UUID bindingId) {
        scriptCalculatedFieldService.deleteBinding(bindingId);
        return ResponseEntity.noContent().build();
    }

    // === Execute by Key (for automation integration) ===

    @PostMapping("/execute-by-key/{scriptKey}")
    @Operation(summary = "Execute a script by key with provided context (for automation/external integration)")
    public ResponseEntity<ScriptConsoleResponse> executeByKey(
            @PathVariable String scriptKey,
            @RequestBody(required = false) Map<String, Object> context,
            jakarta.servlet.http.HttpServletRequest request) {
        Map<String, Object> ctx = context != null ? new java.util.HashMap<>(context) : new java.util.HashMap<>();
        // Pass request headers to script context for webhook API
        Map<String, String> headers = new java.util.LinkedHashMap<>();
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (!name.equalsIgnoreCase("cookie") && !name.equalsIgnoreCase("authorization")) {
                headers.put(name, request.getHeader(name));
            }
        }
        ctx.put("_requestHeaders", headers);
        ScriptResult result = scriptExecutionService.executeByKey(scriptKey, ctx, "API");
        return ResponseEntity.ok(ScriptConsoleResponse.builder()
                .success(result.success())
                .result(result.value())
                .errorMessage(result.errorMessage())
                .executionMs(result.executionMs())
                .consoleOutput(result.consoleOutput())
                .build());
    }

    // === Script Templates ===

    @GetMapping("/templates")
    @Operation(summary = "Get bundled script templates")
    public ResponseEntity<List<Map<String, Object>>> getTemplates() {
        try {
            var resource = new org.springframework.core.io.ClassPathResource("script-templates.json");
            var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> templates = objectMapper.readValue(
                    resource.getInputStream(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            log.warn("Failed to load script templates: {}", e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    // === Script Search ===

    @GetMapping("/search")
    @Operation(summary = "Search scripts by name, key, or body content")
    public ResponseEntity<List<ScriptResponse>> searchScripts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean enabled) {
        List<ScriptResponse> all = scriptDefinitionService.listScripts(type);
        var filtered = all.stream();
        if (query != null && !query.isBlank()) {
            String q = query.toLowerCase();
            filtered = filtered.filter(s ->
                    (s.getName() != null && s.getName().toLowerCase().contains(q)) ||
                    (s.getScriptKey() != null && s.getScriptKey().toLowerCase().contains(q)) ||
                    (s.getDescription() != null && s.getDescription().toLowerCase().contains(q)));
        }
        if (category != null && !category.isBlank()) {
            filtered = filtered.filter(s -> category.equalsIgnoreCase(s.getCategory()));
        }
        if (enabled != null) {
            filtered = filtered.filter(s -> enabled.equals(s.getIsEnabled()));
        }
        return ResponseEntity.ok(filtered.toList());
    }

    // === Script Usage/Dependencies ===

    @GetMapping("/{id}/usage")
    @Operation(summary = "Find where a script is used (workflows, listeners, behaviors, calculated fields)")
    public ResponseEntity<Map<String, Object>> getScriptUsage(@PathVariable UUID id) {
        ScriptResponse script = scriptDefinitionService.getScript(id);
        String scriptKey = script.getScriptKey();

        Map<String, Object> usage = new java.util.LinkedHashMap<>();
        usage.put("scriptKey", scriptKey);
        usage.put("listeners", scriptListenerService.getListenersForScript(id));
        usage.put("fieldBehaviors", scriptFieldBehaviorService.getBehaviorsForScript(id));
        usage.put("calculatedFields", scriptCalculatedFieldService.getBindingsForScript(id));

        List<ScriptResponse> allScripts = scriptDefinitionService.listScripts(null);
        List<String> includers = allScripts.stream()
                .filter(s -> s.getScriptBody() != null && s.getScriptBody().contains("include(\"" + scriptKey + "\")"))
                .map(ScriptResponse::getScriptKey)
                .toList();
        usage.put("includedBy", includers);

        return ResponseEntity.ok(usage);
    }

    // === Profiler Stats ===

    @GetMapping("/profiler/stats")
    @Operation(summary = "Get script execution profiler statistics")
    public ResponseEntity<Map<String, Object>> getProfilerStats() {
        var allLogs = scriptDefinitionService.getAllExecutionLogs(
                org.springframework.data.domain.PageRequest.of(0, 1000,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
        Map<String, Object> stats = new java.util.LinkedHashMap<>();

        var logs = allLogs.getContent();
        stats.put("totalExecutions", logs.size());
        stats.put("successRate", logs.isEmpty() ? 0 :
                logs.stream().filter(com.avionics_systems.workflow.dto.ScriptExecutionLogResponse::isSuccess).count() * 100.0 / logs.size());

        var byScript = logs.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        com.avionics_systems.workflow.dto.ScriptExecutionLogResponse::getScriptKey,
                        java.util.stream.Collectors.averagingLong(com.avionics_systems.workflow.dto.ScriptExecutionLogResponse::getExecutionMs)));
        var slowest = byScript.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(e -> Map.of("scriptKey", (Object) e.getKey(), "avgMs", e.getValue()))
                .toList();
        stats.put("slowestScripts", slowest);

        var byMode = logs.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        l -> l.getExecutionMode() != null ? l.getExecutionMode() : "UNKNOWN",
                        java.util.stream.Collectors.counting()));
        stats.put("executionsByMode", byMode);

        return ResponseEntity.ok(stats);
    }

    // === Script Version Body ===

    @GetMapping("/{id}/versions/{versionNumber}")
    @Operation(summary = "Get a specific script version's body")
    public ResponseEntity<Map<String, Object>> getVersionBody(
            @PathVariable UUID id, @PathVariable Integer versionNumber) {
        var versions = scriptDefinitionService.getVersionHistory(id);
        return versions.stream()
                .filter(v -> versionNumber.equals(v.getVersion()))
                .findFirst()
                .map(v -> {
                    Map<String, Object> result = new java.util.LinkedHashMap<>();
                    result.put("version", v.getVersion());
                    result.put("scriptBody", v.getScriptBody());
                    result.put("changeSummary", v.getChangeSummary());
                    result.put("createdAt", v.getCreatedAt());
                    result.put("createdBy", v.getCreatedBy());
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // === Debug API ===

    @GetMapping("/debug/sessions")
    @Operation(summary = "List active debug sessions")
    public ResponseEntity<Map<String, Object>> listDebugSessions() {
        return ResponseEntity.ok(com.avionics_systems.workflow.engine.script.ScriptDebugger.listActiveSessions());
    }

    @GetMapping("/debug/state/{sessionId}")
    @Operation(summary = "Get debug session state including breakpoints and variable snapshots")
    public ResponseEntity<Map<String, Object>> getDebugState(@PathVariable String sessionId) {
        var session = com.avionics_systems.workflow.engine.script.ScriptDebugger.getSession(sessionId);
        if (session == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(session.getState());
    }

    @PostMapping("/debug/resume/{sessionId}")
    @Operation(summary = "Resume a paused debug session")
    public ResponseEntity<Map<String, Object>> resumeDebug(@PathVariable String sessionId) {
        var session = com.avionics_systems.workflow.engine.script.ScriptDebugger.getSession(sessionId);
        if (session == null) return ResponseEntity.notFound().build();
        session.resume();
        return ResponseEntity.ok(Map.of("resumed", true, "sessionId", sessionId));
    }
}
