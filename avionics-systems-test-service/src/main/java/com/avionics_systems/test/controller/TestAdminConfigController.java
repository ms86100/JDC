package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.service.TestAdminConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/test-admin")
@RequiredArgsConstructor
@Tag(name = "Test Admin Configuration", description = "APIs for managing test status, execution status, and test type configurations")
public class TestAdminConfigController {

    private final TestAdminConfigService testAdminConfigService;

    // ===================== Test Status Config =====================

    @GetMapping("/statuses")
    @Operation(summary = "Get all active test status configurations")
    public ResponseEntity<List<TestStatusConfigResponse>> getAllStatuses() {
        List<TestStatusConfigResponse> statuses = testAdminConfigService.getAllStatuses();
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/statuses/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get test status configurations for a project")
    public ResponseEntity<List<TestStatusConfigResponse>> getStatusesByProject(@PathVariable UUID projectId) {
        List<TestStatusConfigResponse> statuses = testAdminConfigService.getStatusesByProject(projectId);
        return ResponseEntity.ok(statuses);
    }

    @PostMapping("/statuses")
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #request.projectId)")
    @Operation(summary = "Create a new test status configuration")
    public ResponseEntity<TestStatusConfigResponse> createStatus(@Valid @RequestBody TestStatusConfigRequest request) {
        TestStatusConfigResponse status = testAdminConfigService.createStatus(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(status);
    }

    @PutMapping("/statuses/{id}")
    @Operation(summary = "Update a test status configuration")
    public ResponseEntity<TestStatusConfigResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody TestStatusConfigRequest request) {
        TestStatusConfigResponse status = testAdminConfigService.updateStatus(id, request);
        return ResponseEntity.ok(status);
    }

    @DeleteMapping("/statuses/{id}")
    @Operation(summary = "Delete a test status configuration")
    public ResponseEntity<Void> deleteStatus(@PathVariable UUID id) {
        testAdminConfigService.deleteStatus(id);
        return ResponseEntity.noContent().build();
    }

    // ===================== Execution Status Config =====================

    @GetMapping("/execution-statuses")
    @Operation(summary = "Get all active execution status configurations")
    public ResponseEntity<List<ExecutionStatusConfigResponse>> getAllExecutionStatuses() {
        List<ExecutionStatusConfigResponse> statuses = testAdminConfigService.getAllExecutionStatuses();
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/execution-statuses/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get execution status configurations for a project")
    public ResponseEntity<List<ExecutionStatusConfigResponse>> getExecutionStatusesByProject(@PathVariable UUID projectId) {
        List<ExecutionStatusConfigResponse> statuses = testAdminConfigService.getExecutionStatusesByProject(projectId);
        return ResponseEntity.ok(statuses);
    }

    @PostMapping("/execution-statuses")
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #request.projectId)")
    @Operation(summary = "Create a new execution status configuration")
    public ResponseEntity<ExecutionStatusConfigResponse> createExecutionStatus(@Valid @RequestBody ExecutionStatusConfigRequest request) {
        ExecutionStatusConfigResponse status = testAdminConfigService.createExecutionStatus(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(status);
    }

    @PutMapping("/execution-statuses/{id}")
    @Operation(summary = "Update an execution status configuration")
    public ResponseEntity<ExecutionStatusConfigResponse> updateExecutionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ExecutionStatusConfigRequest request) {
        ExecutionStatusConfigResponse status = testAdminConfigService.updateExecutionStatus(id, request);
        return ResponseEntity.ok(status);
    }

    @DeleteMapping("/execution-statuses/{id}")
    @Operation(summary = "Delete an execution status configuration")
    public ResponseEntity<Void> deleteExecutionStatus(@PathVariable UUID id) {
        testAdminConfigService.deleteExecutionStatus(id);
        return ResponseEntity.noContent().build();
    }

    // ===================== Test Type Config =====================

    @GetMapping("/test-types")
    @Operation(summary = "Get all active test type configurations")
    public ResponseEntity<List<TestTypeConfigResponse>> getAllTestTypes() {
        List<TestTypeConfigResponse> types = testAdminConfigService.getAllTestTypes();
        return ResponseEntity.ok(types);
    }

    @GetMapping("/test-types/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get test type configurations for a project")
    public ResponseEntity<List<TestTypeConfigResponse>> getTestTypesByProject(@PathVariable UUID projectId) {
        List<TestTypeConfigResponse> types = testAdminConfigService.getTestTypesByProject(projectId);
        return ResponseEntity.ok(types);
    }

    @PostMapping("/test-types")
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #request.projectId)")
    @Operation(summary = "Create a new test type configuration")
    public ResponseEntity<TestTypeConfigResponse> createTestType(@Valid @RequestBody TestTypeConfigRequest request) {
        TestTypeConfigResponse type = testAdminConfigService.createTestType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(type);
    }

    @PutMapping("/test-types/{id}")
    @Operation(summary = "Update a test type configuration")
    public ResponseEntity<TestTypeConfigResponse> updateTestType(
            @PathVariable UUID id,
            @Valid @RequestBody TestTypeConfigRequest request) {
        TestTypeConfigResponse type = testAdminConfigService.updateTestType(id, request);
        return ResponseEntity.ok(type);
    }

    @DeleteMapping("/test-types/{id}")
    @Operation(summary = "Delete a test type configuration")
    public ResponseEntity<Void> deleteTestType(@PathVariable UUID id) {
        testAdminConfigService.deleteTestType(id);
        return ResponseEntity.noContent().build();
    }
}
