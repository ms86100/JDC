package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.CreateTechEventRequest;
import com.avionics_systems.test.dto.TechEventResponse;
import com.avionics_systems.test.service.DefectManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tech-events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tech Event Management", description = "System anomaly management per Airbus M1668")
public class TechEventController {

    private final DefectManagementService defectManagementService;

    @PostMapping
    @Operation(summary = "Create a new Tech Event")
    public ResponseEntity<TechEventResponse> create(@Valid @RequestBody CreateTechEventRequest request) {
        log.info("POST /api/tech-events - Creating TechEvent for project: {}", request.getProjectId());
        TechEventResponse response = defectManagementService.createTechEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a Tech Event by ID")
    public ResponseEntity<TechEventResponse> getById(@PathVariable UUID id) {
        log.info("GET /api/tech-events/{}", id);
        TechEventResponse response = defectManagementService.getTechEvent(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List Tech Events by project")
    public ResponseEntity<List<TechEventResponse>> getByProject(@PathVariable UUID projectId) {
        log.info("GET /api/tech-events/project/{}", projectId);
        List<TechEventResponse> responses = defectManagementService.getTechEventsByProject(projectId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a Tech Event")
    public ResponseEntity<TechEventResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateTechEventRequest request) {
        log.info("PUT /api/tech-events/{}", id);
        TechEventResponse response = defectManagementService.updateTechEvent(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-program/{programId}")
    @Operation(summary = "List Tech Events by detected program")
    public ResponseEntity<List<TechEventResponse>> getByProgram(@PathVariable UUID programId) {
        log.info("GET /api/tech-events/by-program/{}", programId);
        List<TechEventResponse> responses = defectManagementService.getTechEventsByProgram(programId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/by-status/{status}")
    @Operation(summary = "List Tech Events by status")
    public ResponseEntity<List<TechEventResponse>> getByStatus(@PathVariable String status) {
        log.info("GET /api/tech-events/by-status/{}", status);
        List<TechEventResponse> responses = defectManagementService.getTechEventsByStatus(status);
        return ResponseEntity.ok(responses);
    }
}
