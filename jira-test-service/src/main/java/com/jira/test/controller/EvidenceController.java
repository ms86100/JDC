package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.service.EvidenceManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/evidence")
@RequiredArgsConstructor
@Tag(name = "Evidence Management", description = "APIs for test evidence management lifecycle")
public class EvidenceController {

    private final EvidenceManagementService evidenceService;

    // ==================== Evidence Operations ====================

    @PostMapping
    @Operation(summary = "Upload evidence for an execution")
    public ResponseEntity<EvidenceResponse> uploadEvidence(@Valid @RequestBody EvidenceUploadRequest request) {
        EvidenceResponse response = evidenceService.uploadEvidence(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{evidenceId}")
    @Operation(summary = "Get evidence by ID")
    public ResponseEntity<EvidenceResponse> getEvidence(@PathVariable UUID evidenceId) {
        EvidenceResponse response = evidenceService.getEvidence(evidenceId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/execution/{executionId}")
    @Operation(summary = "Get all evidence for an execution")
    public ResponseEntity<List<EvidenceResponse>> getEvidenceForExecution(@PathVariable UUID executionId) {
        List<EvidenceResponse> evidences = evidenceService.getEvidenceForExecution(executionId);
        return ResponseEntity.ok(evidences);
    }

    @GetMapping("/execution/{executionId}/viewer")
    @Operation(summary = "Get evidence viewer data for an execution")
    public ResponseEntity<EvidenceViewerData> getViewerData(@PathVariable UUID executionId) {
        EvidenceViewerData viewerData = evidenceService.getViewerData(executionId);
        return ResponseEntity.ok(viewerData);
    }

    @GetMapping("/step/{stepResultId}")
    @Operation(summary = "Get all evidence for a step")
    public ResponseEntity<List<EvidenceResponse>> getEvidenceForStep(@PathVariable UUID stepResultId) {
        List<EvidenceResponse> evidences = evidenceService.getEvidenceForStep(stepResultId);
        return ResponseEntity.ok(evidences);
    }

    @PutMapping("/classify")
    @Operation(summary = "Classify evidence (step/run/environment level)")
    public ResponseEntity<EvidenceResponse> classifyEvidence(@Valid @RequestBody EvidenceClassificationRequest request) {
        EvidenceResponse response = evidenceService.classifyEvidence(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{evidenceId}")
    @Operation(summary = "Delete evidence")
    public ResponseEntity<Void> deleteEvidence(@PathVariable UUID evidenceId) {
        evidenceService.deleteEvidence(evidenceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{evidenceId}/archive")
    @Operation(summary = "Archive evidence")
    public ResponseEntity<Void> archiveEvidence(@PathVariable UUID evidenceId) {
        evidenceService.archiveEvidence(evidenceId);
        return ResponseEntity.ok().build();
    }

    // ==================== Retention Policies ====================

    @PostMapping("/policies")
    @Operation(summary = "Create retention policy")
    public ResponseEntity<RetentionPolicyResponse> createPolicy(@Valid @RequestBody RetentionPolicyRequest request) {
        RetentionPolicyResponse response = evidenceService.createRetentionPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/policies")
    @Operation(summary = "Get retention policies")
    public ResponseEntity<List<RetentionPolicyResponse>> getPolicies(@RequestParam(required = false) UUID projectId) {
        List<RetentionPolicyResponse> policies = evidenceService.getRetentionPolicies(projectId);
        return ResponseEntity.ok(policies);
    }

    @GetMapping("/policies/{policyId}")
    @Operation(summary = "Get retention policy by ID")
    public ResponseEntity<RetentionPolicyResponse> getPolicy(@PathVariable UUID policyId) {
        RetentionPolicyResponse response = evidenceService.getRetentionPolicy(policyId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/policies/{policyId}/apply")
    @Operation(summary = "Apply retention policy")
    public ResponseEntity<Void> applyPolicy(@PathVariable UUID policyId) {
        evidenceService.applyRetentionPolicy(policyId);
        return ResponseEntity.ok().build();
    }
}