package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.service.EnvironmentMatrixService;
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
@RequestMapping("/api/environment-matrix")
@RequiredArgsConstructor
@Tag(name = "Environment Matrix", description = "APIs for environment matrix configuration and provisioning")
public class EnvironmentMatrixController {

    private final EnvironmentMatrixService matrixService;

    // ==================== Matrix Configuration ====================

    @PostMapping
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #request.projectId)")
    @Operation(summary = "Create environment matrix")
    public ResponseEntity<MatrixConfigurationResponse> createMatrix(@Valid @RequestBody MatrixConfigurationRequest request) {
        MatrixConfigurationResponse response = matrixService.createMatrix(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all matrices for a project")
    public ResponseEntity<List<MatrixConfigurationResponse>> getMatrices(@RequestParam UUID projectId) {
        List<MatrixConfigurationResponse> matrices = matrixService.getMatrices(projectId);
        return ResponseEntity.ok(matrices);
    }

    @GetMapping("/{matrixId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get matrix by ID")
    public ResponseEntity<MatrixConfigurationResponse> getMatrix(@PathVariable UUID matrixId, @RequestParam UUID projectId) {
        MatrixConfigurationResponse response = matrixService.getMatrix(matrixId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{matrixId}")
    @PreAuthorize("@projectSecurity.canDeleteTests(authentication, #projectId)")
    @Operation(summary = "Delete matrix")
    public ResponseEntity<Void> deleteMatrix(@PathVariable UUID matrixId, @RequestParam UUID projectId) {
        matrixService.deleteMatrix(matrixId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Matrix Visualization ====================

    @GetMapping("/{matrixId}/visualization")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get matrix visualization data (heatmap, distribution)")
    public ResponseEntity<EnvironmentMatrixService.MatrixVisualizationData> getVisualizationData(
            @PathVariable UUID matrixId,
            @RequestParam UUID projectId) {
        EnvironmentMatrixService.MatrixVisualizationData visualization = matrixService.getVisualizationData(matrixId);
        return ResponseEntity.ok(visualization);
    }

    // ==================== Compatibility Checking ====================

    @PostMapping("/compatibility-check")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Check compatibility of test requirements against matrix")
    public ResponseEntity<EnvironmentMatrixService.CompatibilityCheckResult> checkCompatibility(
            @RequestParam UUID projectId,
            @RequestParam UUID matrixId,
            @RequestBody Map<String, String> testRequirements) {
        EnvironmentMatrixService.CompatibilityCheckResult result = matrixService.checkCompatibility(matrixId, testRequirements);
        return ResponseEntity.ok(result);
    }

    // ==================== Matrix-Based Test Execution ====================

    @PostMapping("/{matrixId}/execution-plan")
    @PreAuthorize("@projectSecurity.canExecuteTests(authentication, #projectId)")
    @Operation(summary = "Generate test execution plan for matrix")
    public ResponseEntity<EnvironmentMatrixService.TestExecutionPlan> generateExecutionPlan(
            @PathVariable UUID matrixId,
            @RequestParam UUID projectId,
            @RequestParam UUID testId,
            @RequestBody List<UUID> testCaseIds) {
        EnvironmentMatrixService.TestExecutionPlan plan = matrixService.generateExecutionPlan(matrixId, testId, testCaseIds);
        return ResponseEntity.ok(plan);
    }

    // ==================== Cloud Provider Integration ====================

    @GetMapping("/cloud-providers")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get available cloud providers")
    public ResponseEntity<EnvironmentMatrixService.CloudProviderInfo> getCloudProviders(@RequestParam UUID projectId) {
        EnvironmentMatrixService.CloudProviderInfo providers = matrixService.getCloudProviders();
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/cloud-providers/{providerType}/capabilities")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get capabilities for a specific cloud provider")
    public ResponseEntity<Map<String, Object>> getProviderCapabilities(
            @PathVariable String providerType,
            @RequestParam UUID projectId) {
        Map<String, Object> capabilities = matrixService.getProviderCapabilities(providerType);
        return ResponseEntity.ok(capabilities);
    }

    // ==================== Combination Operations ====================

    @GetMapping("/{matrixId}/combinations")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all combinations for a matrix")
    public ResponseEntity<List<CombinationResponse>> getCombinations(@PathVariable UUID matrixId, @RequestParam UUID projectId) {
        List<CombinationResponse> combinations = matrixService.getCombinations(matrixId);
        return ResponseEntity.ok(combinations);
    }

    @GetMapping("/{matrixId}/combinations/valid")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get valid combinations only")
    public ResponseEntity<List<CombinationResponse>> getValidCombinations(@PathVariable UUID matrixId, @RequestParam UUID projectId) {
        List<CombinationResponse> combinations = matrixService.getValidCombinations(matrixId);
        return ResponseEntity.ok(combinations);
    }

    @PostMapping("/{matrixId}/validate")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Validate all combinations in matrix")
    public ResponseEntity<List<CombinationResponse>> validateCombinations(@PathVariable UUID matrixId, @RequestParam UUID projectId) {
        List<CombinationResponse> combinations = matrixService.validateCombinations(matrixId);
        return ResponseEntity.ok(combinations);
    }

    // ==================== Provisioning ====================

    @PostMapping("/provision")
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #request.projectId)")
    @Operation(summary = "Provision environment for a combination")
    public ResponseEntity<ProvisionResponse> provisionEnvironment(@Valid @RequestBody EnvironmentProvisionRequest request) {
        ProvisionResponse response = matrixService.provisionEnvironment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/combinations/{combinationId}/provisioned")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get provisioned environment config")
    public ResponseEntity<ProvisionResponse> getProvisionedEnvironment(
            @PathVariable UUID combinationId,
            @RequestParam UUID projectId) {
        ProvisionResponse response = matrixService.getProvisionedEnvironment(combinationId);
        return ResponseEntity.ok(response);
    }

    // ==================== Provisioning Workflow ====================

    @PostMapping("/{matrixId}/provisioning-workflow")
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #projectId)")
    @Operation(summary = "Start provisioning workflow for matrix combinations")
    public ResponseEntity<EnvironmentMatrixService.ProvisioningWorkflowResponse> startProvisioningWorkflow(
            @PathVariable UUID matrixId,
            @RequestParam UUID projectId,
            @RequestBody(required = false) List<UUID> combinationIds) {
        EnvironmentMatrixService.ProvisioningWorkflowResponse response =
                matrixService.startProvisioningWorkflow(matrixId, combinationIds != null ? combinationIds : List.of());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/provisioning-workflow/{workflowId}/status")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get provisioning workflow status")
    public ResponseEntity<EnvironmentMatrixService.ProvisioningWorkflowStatus> getWorkflowStatus(
            @PathVariable UUID workflowId,
            @RequestParam UUID projectId) {
        EnvironmentMatrixService.ProvisioningWorkflowStatus status = matrixService.getWorkflowStatus(workflowId);
        return ResponseEntity.ok(status);
    }

    // ==================== Provisioning Rules ====================

    @PostMapping("/rules")
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #request.projectId)")
    @Operation(summary = "Create provisioning rule")
    public ResponseEntity<ProvisioningRuleResponse> createRule(@Valid @RequestBody ProvisioningRuleRequest request) {
        ProvisioningRuleResponse response = matrixService.createProvisioningRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/rules")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get provisioning rules")
    public ResponseEntity<List<ProvisioningRuleResponse>> getRules(@RequestParam(required = false) UUID projectId) {
        List<ProvisioningRuleResponse> rules = matrixService.getProvisioningRules(projectId);
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/rules/{ruleId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get provisioning rule by ID")
    public ResponseEntity<ProvisioningRuleResponse> getRule(@PathVariable UUID ruleId, @RequestParam UUID projectId) {
        ProvisioningRuleResponse response = matrixService.getProvisioningRule(ruleId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rules/{ruleId}")
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #projectId)")
    @Operation(summary = "Delete provisioning rule")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID ruleId, @RequestParam UUID projectId) {
        matrixService.deleteProvisioningRule(ruleId);
        return ResponseEntity.noContent().build();
    }
}