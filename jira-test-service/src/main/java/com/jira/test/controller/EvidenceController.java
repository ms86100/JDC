package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.service.EvidenceManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/evidence")
@RequiredArgsConstructor
@Tag(name = "Evidence Management", description = "APIs for test evidence management lifecycle")
public class EvidenceController {

    private final EvidenceManagementService evidenceService;

    // ==================== Evidence Operations ====================

    @PostMapping
    @PreAuthorize("@projectSecurity.canExecuteTests(authentication, #request.projectId)")
    @Operation(summary = "Upload evidence for an execution")
    public ResponseEntity<EvidenceResponse> uploadEvidence(@Valid @RequestBody EvidenceUploadRequest request) {
        EvidenceResponse response = evidenceService.uploadEvidence(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{evidenceId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get evidence by ID")
    public ResponseEntity<EvidenceResponse> getEvidence(@PathVariable UUID evidenceId, @RequestParam UUID projectId) {
        EvidenceResponse response = evidenceService.getEvidence(evidenceId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/execution/{executionId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all evidence for an execution")
    public ResponseEntity<List<EvidenceResponse>> getEvidenceForExecution(@PathVariable UUID executionId, @RequestParam UUID projectId) {
        List<EvidenceResponse> evidences = evidenceService.getEvidenceForExecution(executionId);
        return ResponseEntity.ok(evidences);
    }

    @GetMapping("/execution/{executionId}/viewer")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get evidence viewer data for an execution")
    public ResponseEntity<EvidenceViewerData> getViewerData(@PathVariable UUID executionId, @RequestParam UUID projectId) {
        EvidenceViewerData viewerData = evidenceService.getViewerData(executionId);
        return ResponseEntity.ok(viewerData);
    }

    @GetMapping("/step/{stepResultId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all evidence for a step")
    public ResponseEntity<List<EvidenceResponse>> getEvidenceForStep(@PathVariable UUID stepResultId, @RequestParam UUID projectId) {
        List<EvidenceResponse> evidences = evidenceService.getEvidenceForStep(stepResultId);
        return ResponseEntity.ok(evidences);
    }

    @PutMapping("/classify")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #request.projectId)")
    @Operation(summary = "Classify evidence (step/run/environment level)")
    public ResponseEntity<EvidenceResponse> classifyEvidence(@Valid @RequestBody EvidenceClassificationRequest request) {
        EvidenceResponse response = evidenceService.classifyEvidence(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{evidenceId}/categorize")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Categorize evidence with tags")
    public ResponseEntity<EvidenceResponse> categorizeEvidence(
            @PathVariable UUID evidenceId,
            @RequestParam UUID projectId,
            @RequestParam String category,
            @RequestBody(required = false) Map<String, String> tags) {
        EvidenceResponse response = evidenceService.categorizeEvidence(evidenceId, category, tags);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{evidenceId}")
    @PreAuthorize("@projectSecurity.canDeleteTests(authentication, #projectId)")
    @Operation(summary = "Delete evidence")
    public ResponseEntity<Void> deleteEvidence(@PathVariable UUID evidenceId, @RequestParam UUID projectId) {
        evidenceService.deleteEvidence(evidenceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{evidenceId}/archive")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Archive evidence")
    public ResponseEntity<Void> archiveEvidence(@PathVariable UUID evidenceId, @RequestParam UUID projectId) {
        evidenceService.archiveEvidence(evidenceId);
        return ResponseEntity.ok().build();
    }

    // ==================== Evidence Linking ====================

    @PostMapping("/{evidenceId}/link/step/{stepResultId}")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Link evidence to a test step")
    public ResponseEntity<Void> linkToStep(
            @PathVariable UUID evidenceId,
            @PathVariable UUID stepResultId,
            @RequestParam UUID projectId) {
        evidenceService.linkEvidenceToStep(evidenceId, stepResultId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{evidenceId}/link/testcase/{testCaseId}")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Link evidence to a test case")
    public ResponseEntity<Void> linkToTestCase(
            @PathVariable UUID evidenceId,
            @PathVariable UUID testCaseId,
            @RequestParam UUID projectId) {
        evidenceService.linkEvidenceToTestCase(evidenceId, testCaseId);
        return ResponseEntity.ok().build();
    }

    // ==================== Chain of Custody ====================

    @GetMapping("/{evidenceId}/custody")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get chain of custody record for evidence")
    public ResponseEntity<EvidenceManagementService.ChainOfCustodyRecord> getChainOfCustody(
            @PathVariable UUID evidenceId,
            @RequestParam UUID projectId) {
        EvidenceManagementService.ChainOfCustodyRecord custody = evidenceService.getChainOfCustody(evidenceId);
        return ResponseEntity.ok(custody);
    }

    @PostMapping("/{evidenceId}/custody")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Add chain of custody event")
    public ResponseEntity<EvidenceManagementService.ChainOfCustodyRecord> addCustodyEvent(
            @PathVariable UUID evidenceId,
            @RequestParam UUID projectId,
            @RequestParam String action,
            @RequestParam String performedBy,
            @RequestParam(required = false) String notes) {
        EvidenceManagementService.ChainOfCustodyRecord record = evidenceService.addCustodyEvent(
                evidenceId, action, performedBy, notes);
        return ResponseEntity.ok(record);
    }

    // ==================== Search ====================

    @PostMapping("/search")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #request.projectId)")
    @Operation(summary = "Search evidence with filters")
    public ResponseEntity<EvidenceSearchResult> searchEvidence(@RequestBody EvidenceSearchRequest request) {
        EvidenceSearchResult results = evidenceService.searchEvidence(request);
        return ResponseEntity.ok(results);
    }

    // ==================== Retention Policies ====================

    @PostMapping("/policies")
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #request.projectId)")
    @Operation(summary = "Create retention policy")
    public ResponseEntity<RetentionPolicyResponse> createPolicy(@Valid @RequestBody RetentionPolicyRequest request) {
        RetentionPolicyResponse response = evidenceService.createRetentionPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/policies")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get retention policies")
    public ResponseEntity<List<RetentionPolicyResponse>> getPolicies(@RequestParam(required = false) UUID projectId) {
        List<RetentionPolicyResponse> policies = evidenceService.getRetentionPolicies(projectId);
        return ResponseEntity.ok(policies);
    }

    @GetMapping("/policies/{policyId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get retention policy by ID")
    public ResponseEntity<RetentionPolicyResponse> getPolicy(@PathVariable UUID policyId, @RequestParam UUID projectId) {
        RetentionPolicyResponse response = evidenceService.getRetentionPolicy(policyId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/policies/{policyId}/apply")
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #projectId)")
    @Operation(summary = "Apply retention policy")
    public ResponseEntity<Void> applyPolicy(@PathVariable UUID policyId, @RequestParam UUID projectId) {
        evidenceService.applyRetentionPolicy(policyId);
        return ResponseEntity.ok().build();
    }
}