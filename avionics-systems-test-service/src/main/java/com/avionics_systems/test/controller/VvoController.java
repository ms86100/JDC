package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.service.VvoService;
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
@RequestMapping("/api/vvo")
@RequiredArgsConstructor
@Tag(name = "VVO Management", description = "Verification & Validation Objectives")
public class VvoController {

    private final VvoService vvoService;

    @PostMapping
    @Operation(summary = "Create VVO")
    public ResponseEntity<VvoResponse> createVvo(@Valid @RequestBody CreateVvoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vvoService.createVvo(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get VVO by ID")
    public ResponseEntity<VvoResponse> getVvo(@PathVariable UUID id) {
        return ResponseEntity.ok(vvoService.getVvo(id));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List VVOs by project")
    public ResponseEntity<List<VvoResponse>> getVvosByProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(vvoService.getVvosByProject(projectId));
    }

    @GetMapping("/hlvvo/{hlvvoId}")
    @Operation(summary = "List VVOs by HLVVO parent")
    public ResponseEntity<List<VvoResponse>> getVvosByHlvvo(@PathVariable UUID hlvvoId) {
        return ResponseEntity.ok(vvoService.getVvosByHlvvo(hlvvoId));
    }

    @GetMapping("/by-doors-id/{idDoors}")
    @Operation(summary = "Get VVO by DOORS ID")
    public ResponseEntity<VvoResponse> getVvoByDoorsId(@PathVariable String idDoors) {
        return ResponseEntity.ok(vvoService.getVvoByDoorsId(idDoors));
    }

    @GetMapping("/by-fix-version/{fixVersionId}")
    @Operation(summary = "List VVOs by baseline fix version")
    public ResponseEntity<List<VvoResponse>> getVvosByFixVersion(@PathVariable UUID fixVersionId) {
        return ResponseEntity.ok(vvoService.getVvosByFixVersion(fixVersionId));
    }

    @GetMapping("/project/{projectId}/by-statuses")
    @Operation(summary = "List VVOs by project and statuses")
    public ResponseEntity<List<VvoResponse>> getVvosByStatuses(
            @PathVariable UUID projectId,
            @RequestParam List<String> statuses) {
        return ResponseEntity.ok(vvoService.getVvosByStatuses(projectId, statuses));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update VVO")
    public ResponseEntity<VvoResponse> updateVvo(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVvoRequest request) {
        return ResponseEntity.ok(vvoService.updateVvo(id, request));
    }

    @PostMapping("/{id}/clone")
    @Operation(summary = "Clone VVO with version increment")
    public ResponseEntity<VvoCloneResponse> cloneVvo(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vvoService.cloneVvo(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Archive VVO")
    public ResponseEntity<Void> archiveVvo(@PathVariable UUID id) {
        vvoService.archiveVvo(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{vvoId}/test-requests/{testRequestId}")
    @Operation(summary = "Link VVO to Test Request")
    public ResponseEntity<Void> linkToTestRequest(
            @PathVariable UUID vvoId,
            @PathVariable UUID testRequestId) {
        vvoService.linkVvoToTestRequest(vvoId, testRequestId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{vvoId}/test-requests/{testRequestId}")
    @Operation(summary = "Unlink VVO from Test Request")
    public ResponseEntity<Void> unlinkFromTestRequest(
            @PathVariable UUID vvoId,
            @PathVariable UUID testRequestId) {
        vvoService.unlinkVvoFromTestRequest(vvoId, testRequestId);
        return ResponseEntity.noContent().build();
    }
}
