package com.avionics_systems.migration.controller;

import com.avionics_systems.migration.entity.OptionMapping;
import com.avionics_systems.migration.entity.UserMapping;
import com.avionics_systems.migration.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/migration/mapping-engine")
@RequiredArgsConstructor
@Tag(name = "Migration Mapping Engine", description = "Phase 3 option, workflow/status, user, and default mappings")
public class MigrationMappingController {

    private final OptionMappingService optionMappingService;
    private final WorkflowStatusMappingService workflowStatusMappingService;
    private final UserDirectoryMappingService userDirectoryMappingService;
    private final ImportWizardSessionService wizardSessionService;

    @GetMapping("/jobs/{jobId}/option-mappings")
    public ResponseEntity<List<OptionMapping>> getJobOptionMappings(@PathVariable UUID jobId) {
        return ResponseEntity.ok(optionMappingService.getForJob(jobId));
    }

    @PutMapping("/jobs/{jobId}/option-mappings")
    public ResponseEntity<List<OptionMapping>> saveJobOptionMappings(
            @PathVariable UUID jobId,
            @RequestBody List<Map<String, Object>> mappings) {
        return ResponseEntity.ok(optionMappingService.saveForJob(jobId, mappings));
    }

    @GetMapping("/sessions/{sessionId}/option-mappings")
    public ResponseEntity<List<OptionMapping>> getSessionOptionMappings(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(optionMappingService.getForSession(sessionId));
    }

    @PutMapping("/sessions/{sessionId}/option-mappings")
    public ResponseEntity<List<OptionMapping>> saveSessionOptionMappings(
            @PathVariable UUID sessionId,
            @RequestBody List<Map<String, Object>> mappings) {
        return ResponseEntity.ok(optionMappingService.saveForSession(sessionId, mappings));
    }

    @GetMapping("/jobs/{jobId}/workflow-status-mappings")
    public ResponseEntity<Map<String, Object>> getJobWorkflowMappings(@PathVariable UUID jobId) {
        return ResponseEntity.ok(workflowStatusMappingService.getForJob(jobId));
    }

    @PutMapping("/jobs/{jobId}/workflow-status-mappings")
    public ResponseEntity<Map<String, Object>> saveJobWorkflowMappings(
            @PathVariable UUID jobId,
            @RequestBody Map<String, Object> mappings) {
        return ResponseEntity.ok(workflowStatusMappingService.saveForJob(jobId, mappings));
    }

    @PutMapping("/sessions/{sessionId}/workflow-status-mappings")
    public ResponseEntity<Map<String, Object>> saveSessionWorkflowMappings(
            @PathVariable UUID sessionId,
            @RequestBody Map<String, Object> mappings) {
        return ResponseEntity.ok(workflowStatusMappingService.saveForSession(sessionId, mappings));
    }

    @PostMapping("/jobs/{jobId}/resolve-users")
    @Operation(summary = "Resolve source users via user-service directory")
    public ResponseEntity<List<UserMapping>> resolveUsers(
            @PathVariable UUID jobId,
            @RequestBody List<String> sourceIdentifiers) {
        return ResponseEntity.ok(userDirectoryMappingService.resolveSourceUsers(sourceIdentifiers, jobId));
    }

    @PatchMapping("/sessions/{sessionId}/field-defaults")
    public ResponseEntity<?> saveSessionFieldDefaults(
            @PathVariable UUID sessionId,
            @RequestBody Map<String, Object> defaults) {
        wizardSessionService.updateFieldDefaults(sessionId, defaults);
        return ResponseEntity.ok(Map.of("sessionId", sessionId, "saved", true));
    }
}
