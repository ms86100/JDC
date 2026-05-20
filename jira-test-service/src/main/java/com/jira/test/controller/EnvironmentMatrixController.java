package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.service.EnvironmentMatrixService;
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
@RequestMapping("/api/environment-matrix")
@RequiredArgsConstructor
@Tag(name = "Environment Matrix", description = "APIs for environment matrix configuration and provisioning")
public class EnvironmentMatrixController {

    private final EnvironmentMatrixService matrixService;

    // ==================== Matrix Configuration ====================

    @PostMapping
    @Operation(summary = "Create environment matrix")
    public ResponseEntity<MatrixConfigurationResponse> createMatrix(@Valid @RequestBody MatrixConfigurationRequest request) {
        MatrixConfigurationResponse response = matrixService.createMatrix(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all matrices for a project")
    public ResponseEntity<List<MatrixConfigurationResponse>> getMatrices(@RequestParam UUID projectId) {
        List<MatrixConfigurationResponse> matrices = matrixService.getMatrices(projectId);
        return ResponseEntity.ok(matrices);
    }

    @GetMapping("/{matrixId}")
    @Operation(summary = "Get matrix by ID")
    public ResponseEntity<MatrixConfigurationResponse> getMatrix(@PathVariable UUID matrixId) {
        MatrixConfigurationResponse response = matrixService.getMatrix(matrixId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{matrixId}")
    @Operation(summary = "Delete matrix")
    public ResponseEntity<Void> deleteMatrix(@PathVariable UUID matrixId) {
        matrixService.deleteMatrix(matrixId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Combination Operations ====================

    @GetMapping("/{matrixId}/combinations")
    @Operation(summary = "Get all combinations for a matrix")
    public ResponseEntity<List<CombinationResponse>> getCombinations(@PathVariable UUID matrixId) {
        List<CombinationResponse> combinations = matrixService.getCombinations(matrixId);
        return ResponseEntity.ok(combinations);
    }

    @GetMapping("/{matrixId}/combinations/valid")
    @Operation(summary = "Get valid combinations only")
    public ResponseEntity<List<CombinationResponse>> getValidCombinations(@PathVariable UUID matrixId) {
        List<CombinationResponse> combinations = matrixService.getValidCombinations(matrixId);
        return ResponseEntity.ok(combinations);
    }

    @PostMapping("/{matrixId}/validate")
    @Operation(summary = "Validate all combinations in matrix")
    public ResponseEntity<List<CombinationResponse>> validateCombinations(@PathVariable UUID matrixId) {
        List<CombinationResponse> combinations = matrixService.validateCombinations(matrixId);
        return ResponseEntity.ok(combinations);
    }

    // ==================== Provisioning ====================

    @PostMapping("/provision")
    @Operation(summary = "Provision environment for a combination")
    public ResponseEntity<ProvisionResponse> provisionEnvironment(@Valid @RequestBody EnvironmentProvisionRequest request) {
        ProvisionResponse response = matrixService.provisionEnvironment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/combinations/{combinationId}/provisioned")
    @Operation(summary = "Get provisioned environment config")
    public ResponseEntity<ProvisionResponse> getProvisionedEnvironment(@PathVariable UUID combinationId) {
        ProvisionResponse response = matrixService.getProvisionedEnvironment(combinationId);
        return ResponseEntity.ok(response);
    }

    // ==================== Provisioning Rules ====================

    @PostMapping("/rules")
    @Operation(summary = "Create provisioning rule")
    public ResponseEntity<ProvisioningRuleResponse> createRule(@Valid @RequestBody ProvisioningRuleRequest request) {
        ProvisioningRuleResponse response = matrixService.createProvisioningRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/rules")
    @Operation(summary = "Get provisioning rules")
    public ResponseEntity<List<ProvisioningRuleResponse>> getRules(@RequestParam(required = false) UUID projectId) {
        List<ProvisioningRuleResponse> rules = matrixService.getProvisioningRules(projectId);
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/rules/{ruleId}")
    @Operation(summary = "Get provisioning rule by ID")
    public ResponseEntity<ProvisioningRuleResponse> getRule(@PathVariable UUID ruleId) {
        ProvisioningRuleResponse response = matrixService.getProvisioningRule(ruleId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rules/{ruleId}")
    @Operation(summary = "Delete provisioning rule")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID ruleId) {
        matrixService.deleteProvisioningRule(ruleId);
        return ResponseEntity.noContent().build();
    }
}