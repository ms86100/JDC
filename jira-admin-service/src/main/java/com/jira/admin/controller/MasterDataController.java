package com.jira.admin.controller;

import com.jira.admin.dto.masterdata.*;
import com.jira.admin.service.MasterDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/master-data")
@RequiredArgsConstructor
@Tag(name = "Master Data", description = "Aircraft design system master data management")
public class MasterDataController {

    private final MasterDataService masterDataService;

    // ==================== Aircraft Programs ====================

    @GetMapping("/programs")
    @Operation(summary = "List all active aircraft programs")
    public ResponseEntity<List<AircraftProgramResponse>> getAllPrograms() {
        return ResponseEntity.ok(masterDataService.getAllPrograms());
    }

    @PostMapping("/programs")
    @Operation(summary = "Create aircraft program")
    public ResponseEntity<AircraftProgramResponse> createProgram(
            @Valid @RequestBody AircraftProgramRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(masterDataService.createProgram(request));
    }

    @PutMapping("/programs/{id}")
    @Operation(summary = "Update aircraft program")
    public ResponseEntity<AircraftProgramResponse> updateProgram(
            @PathVariable String id,
            @Valid @RequestBody AircraftProgramRequest request) {
        return ResponseEntity.ok(masterDataService.updateProgram(id, request));
    }

    @DeleteMapping("/programs/{id}")
    @Operation(summary = "Deactivate aircraft program")
    public ResponseEntity<Void> deactivateProgram(@PathVariable String id) {
        masterDataService.deactivateProgram(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Test Means ====================

    @GetMapping("/programs/{programId}/test-means")
    @Operation(summary = "List test means for program (cascading)")
    public ResponseEntity<List<TestMeanResponse>> getTestMeansByProgram(
            @PathVariable String programId) {
        return ResponseEntity.ok(masterDataService.getTestMeansByProgram(programId));
    }

    @PostMapping("/test-means")
    @Operation(summary = "Create test mean")
    public ResponseEntity<TestMeanResponse> createTestMean(
            @Valid @RequestBody TestMeanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(masterDataService.createTestMean(request));
    }

    @PutMapping("/test-means/{id}")
    @Operation(summary = "Update test mean")
    public ResponseEntity<TestMeanResponse> updateTestMean(
            @PathVariable String id,
            @Valid @RequestBody TestMeanRequest request) {
        return ResponseEntity.ok(masterDataService.updateTestMean(id, request));
    }

    @DeleteMapping("/test-means/{id}")
    @Operation(summary = "Deactivate test mean")
    public ResponseEntity<Void> deactivateTestMean(@PathVariable String id) {
        masterDataService.deactivateTestMean(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Aircraft Systems ====================

    @GetMapping("/programs/{programId}/systems")
    @Operation(summary = "List aircraft systems for program (cascading)")
    public ResponseEntity<List<AircraftSystemResponse>> getSystemsByProgram(
            @PathVariable String programId) {
        return ResponseEntity.ok(masterDataService.getSystemsByProgram(programId));
    }

    @PostMapping("/systems")
    @Operation(summary = "Create aircraft system")
    public ResponseEntity<AircraftSystemResponse> createSystem(
            @Valid @RequestBody AircraftSystemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(masterDataService.createSystem(request));
    }

    @PutMapping("/systems/{id}")
    @Operation(summary = "Update aircraft system")
    public ResponseEntity<AircraftSystemResponse> updateSystem(
            @PathVariable String id,
            @Valid @RequestBody AircraftSystemRequest request) {
        return ResponseEntity.ok(masterDataService.updateSystem(id, request));
    }

    @DeleteMapping("/systems/{id}")
    @Operation(summary = "Deactivate aircraft system")
    public ResponseEntity<Void> deactivateSystem(@PathVariable String id) {
        masterDataService.deactivateSystem(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== ATA Chapters ====================

    @GetMapping("/programs/{programId}/ata-chapters")
    @Operation(summary = "List ATA chapters for program (cascading)")
    public ResponseEntity<List<AtaChapterResponse>> getAtaChaptersByProgram(
            @PathVariable String programId) {
        return ResponseEntity.ok(masterDataService.getAtaChaptersByProgram(programId));
    }

    @PostMapping("/ata-chapters")
    @Operation(summary = "Create ATA chapter")
    public ResponseEntity<AtaChapterResponse> createAtaChapter(
            @Valid @RequestBody AtaChapterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(masterDataService.createAtaChapter(request));
    }

    @PutMapping("/ata-chapters/{id}")
    @Operation(summary = "Update ATA chapter")
    public ResponseEntity<AtaChapterResponse> updateAtaChapter(
            @PathVariable String id,
            @Valid @RequestBody AtaChapterRequest request) {
        return ResponseEntity.ok(masterDataService.updateAtaChapter(id, request));
    }

    @DeleteMapping("/ata-chapters/{id}")
    @Operation(summary = "Deactivate ATA chapter")
    public ResponseEntity<Void> deactivateAtaChapter(@PathVariable String id) {
        masterDataService.deactivateAtaChapter(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== System Suppliers ====================

    @GetMapping("/programs/{programId}/systems/{systemId}/suppliers")
    @Operation(summary = "List suppliers for program and system (cascading)")
    public ResponseEntity<List<SystemSupplierResponse>> getSuppliersByProgramAndSystem(
            @PathVariable String programId,
            @PathVariable String systemId) {
        return ResponseEntity.ok(masterDataService.getSuppliersByProgramAndSystem(programId, systemId));
    }

    @GetMapping("/programs/{programId}/suppliers")
    @Operation(summary = "List all suppliers for program")
    public ResponseEntity<List<SystemSupplierResponse>> getSuppliersByProgram(
            @PathVariable String programId) {
        return ResponseEntity.ok(masterDataService.getSuppliersByProgram(programId));
    }

    @PostMapping("/suppliers")
    @Operation(summary = "Create system supplier")
    public ResponseEntity<SystemSupplierResponse> createSupplier(
            @Valid @RequestBody SystemSupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(masterDataService.createSupplier(request));
    }

    @PutMapping("/suppliers/{id}")
    @Operation(summary = "Update system supplier")
    public ResponseEntity<SystemSupplierResponse> updateSupplier(
            @PathVariable String id,
            @Valid @RequestBody SystemSupplierRequest request) {
        return ResponseEntity.ok(masterDataService.updateSupplier(id, request));
    }

    @DeleteMapping("/suppliers/{id}")
    @Operation(summary = "Deactivate system supplier")
    public ResponseEntity<Void> deactivateSupplier(@PathVariable String id) {
        masterDataService.deactivateSupplier(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== System Functions ====================

    @GetMapping("/systems/{systemId}/functions")
    @Operation(summary = "List functions for system (cascading)")
    public ResponseEntity<List<SystemFunctionResponse>> getFunctionsBySystem(
            @PathVariable String systemId) {
        return ResponseEntity.ok(masterDataService.getFunctionsBySystem(systemId));
    }

    @PostMapping("/functions")
    @Operation(summary = "Create system function")
    public ResponseEntity<SystemFunctionResponse> createFunction(
            @Valid @RequestBody SystemFunctionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(masterDataService.createFunction(request));
    }

    @PutMapping("/functions/{id}")
    @Operation(summary = "Update system function")
    public ResponseEntity<SystemFunctionResponse> updateFunction(
            @PathVariable String id,
            @Valid @RequestBody SystemFunctionRequest request) {
        return ResponseEntity.ok(masterDataService.updateFunction(id, request));
    }

    @DeleteMapping("/functions/{id}")
    @Operation(summary = "Deactivate system function")
    public ResponseEntity<Void> deactivateFunction(@PathVariable String id) {
        masterDataService.deactivateFunction(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Reporter Teams ====================

    @GetMapping("/reporter-teams")
    @Operation(summary = "List reporter teams, optionally filtered by program")
    public ResponseEntity<List<ReporterTeamResponse>> getAllReporterTeams(
            @RequestParam(required = false) String programId) {
        if (programId != null && !programId.isEmpty()) {
            return ResponseEntity.ok(masterDataService.getReporterTeamsByProgram(programId));
        }
        return ResponseEntity.ok(masterDataService.getAllReporterTeams());
    }

    @PostMapping("/reporter-teams")
    @Operation(summary = "Create reporter team")
    public ResponseEntity<ReporterTeamResponse> createReporterTeam(
            @Valid @RequestBody ReporterTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(masterDataService.createReporterTeam(request));
    }

    @PutMapping("/reporter-teams/{id}")
    @Operation(summary = "Update reporter team")
    public ResponseEntity<ReporterTeamResponse> updateReporterTeam(
            @PathVariable String id,
            @Valid @RequestBody ReporterTeamRequest request) {
        return ResponseEntity.ok(masterDataService.updateReporterTeam(id, request));
    }

    @DeleteMapping("/reporter-teams/{id}")
    @Operation(summary = "Deactivate reporter team")
    public ResponseEntity<Void> deactivateReporterTeam(@PathVariable String id) {
        masterDataService.deactivateReporterTeam(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Test Mean Defect Origins ====================

    @GetMapping("/defect-origins")
    @Operation(summary = "List root-level defect origins")
    public ResponseEntity<List<TestMeanDefectOriginResponse>> getRootDefectOrigins() {
        return ResponseEntity.ok(masterDataService.getRootDefectOrigins());
    }

    @GetMapping("/defect-origins/{parentId}/sub-items")
    @Operation(summary = "List defect origin sub-items for a parent")
    public ResponseEntity<List<TestMeanDefectOriginResponse>> getDefectOriginSubItems(
            @PathVariable String parentId) {
        return ResponseEntity.ok(masterDataService.getDefectOriginSubItems(parentId));
    }

    @PostMapping("/defect-origins")
    @Operation(summary = "Create defect origin")
    public ResponseEntity<TestMeanDefectOriginResponse> createDefectOrigin(
            @Valid @RequestBody TestMeanDefectOriginRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(masterDataService.createDefectOrigin(request));
    }

    @PutMapping("/defect-origins/{id}")
    @Operation(summary = "Update defect origin")
    public ResponseEntity<TestMeanDefectOriginResponse> updateDefectOrigin(
            @PathVariable String id,
            @Valid @RequestBody TestMeanDefectOriginRequest request) {
        return ResponseEntity.ok(masterDataService.updateDefectOrigin(id, request));
    }

    @DeleteMapping("/defect-origins/{id}")
    @Operation(summary = "Deactivate defect origin")
    public ResponseEntity<Void> deactivateDefectOrigin(@PathVariable String id) {
        masterDataService.deactivateDefectOrigin(id);
        return ResponseEntity.noContent().build();
    }
}
