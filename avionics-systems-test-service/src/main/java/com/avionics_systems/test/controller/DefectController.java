package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.DefectLinkResponse;
import com.avionics_systems.test.dto.DefectLinkRequest;
import com.avionics_systems.test.entity.DefectLink;
import com.avionics_systems.test.exception.ResourceNotFoundException;
import com.avionics_systems.test.exception.ValidationException;
import com.avionics_systems.test.repository.DefectLinkRepository;
import com.avionics_systems.test.repository.TestIssueRepository;
import com.avionics_systems.test.service.TraceabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/defects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Defect Tracking", description = "APIs for tracking defects linked to test executions")
public class DefectController {

    private final DefectLinkRepository defectLinkRepository;
    private final TestIssueRepository testIssueRepository;
    private final TraceabilityService traceabilityService;

    @GetMapping
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all defects for a project")
    public ResponseEntity<List<DefectLinkResponse>> getDefects(@RequestParam UUID projectId) {
        log.info("Getting defects for project: {}", projectId);
        List<DefectLink> defectLinks = defectLinkRepository.findAll();
        List<DefectLinkResponse> response = defectLinks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{defectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get a defect by ID")
    public ResponseEntity<DefectLinkResponse> getDefect(@PathVariable UUID defectId) {
        return defectLinkRepository.findById(defectId)
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Defect", "id", defectId));
    }

    @GetMapping("/by-key/{defectKey}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all links for a specific defect key")
    public ResponseEntity<List<DefectLinkResponse>> getDefectLinksByKey(@PathVariable String defectKey) {
        log.info("Getting links for defect: {}", defectKey);
        List<DefectLinkResponse> links = defectLinkRepository.findByDefectKey(defectKey).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(links);
    }

    @GetMapping("/by-execution/{executionId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all defects linked to an execution")
    public ResponseEntity<List<DefectLinkResponse>> getDefectsByExecution(@PathVariable UUID executionId) {
        log.info("Getting defects for execution: {}", executionId);
        List<DefectLinkResponse> defects = defectLinkRepository.findByExecutionId(executionId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(defects);
    }

    @GetMapping("/by-status/{status}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all defects by status")
    public ResponseEntity<List<DefectLinkResponse>> getDefectsByStatus(@PathVariable String status) {
        log.info("Getting defects by status: {}", status);
        List<DefectLinkResponse> defects = defectLinkRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(defects);
    }

    @PostMapping
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #request.projectId)")
    @Operation(summary = "Link a defect to a test execution")
    public ResponseEntity<DefectLinkResponse> linkDefect(
            @RequestParam UUID projectId,
            @Valid @RequestBody DefectLinkRequest request) {
        log.info("Linking defect {} to execution: {}", request.getDefectKey(), request.getExecutionId());

        if (request.getExecutionId() == null && request.getStepResultId() == null) {
            throw new ValidationException("Either executionId or stepResultId must be provided");
        }

        DefectLinkResponse response = traceabilityService.linkDefect(
                request.getExecutionId(),
                request.getStepResultId(),
                request.getDefectKey(),
                request.getSeverity());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{defectId}/status")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Update defect status")
    public ResponseEntity<DefectLinkResponse> updateDefectStatus(
            @PathVariable UUID defectId,
            @RequestParam UUID projectId,
            @RequestParam String status) {
        log.info("Updating defect {} status to: {}", defectId, status);

        DefectLink defect = defectLinkRepository.findById(defectId)
                .orElseThrow(() -> new ResourceNotFoundException("Defect", "id", defectId));

        defect.setStatus(status);
        defect = defectLinkRepository.save(defect);

        return ResponseEntity.ok(mapToResponse(defect));
    }

    @DeleteMapping("/{defectId}")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Remove a defect link")
    public ResponseEntity<Void> deleteDefectLink(
            @PathVariable UUID defectId,
            @RequestParam UUID projectId) {
        log.info("Deleting defect link: {}", defectId);
        defectLinkRepository.deleteById(defectId);
        return ResponseEntity.noContent().build();
    }

    private DefectLinkResponse mapToResponse(DefectLink defect) {
        return DefectLinkResponse.builder()
                .id(defect.getId())
                .defectKey(defect.getDefectKey())
                .executionId(defect.getExecutionId())
                .stepResultId(defect.getStepResultId())
                .severity(defect.getSeverity())
                .status(defect.getStatus())
                .createdAt(defect.getCreatedAt())
                .build();
    }
}