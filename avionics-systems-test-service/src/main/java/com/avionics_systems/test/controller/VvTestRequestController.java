package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.CreateTestRequestRequest;
import com.avionics_systems.test.dto.TestRequestResponse;
import com.avionics_systems.test.dto.VvoResponse;
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
@RequestMapping("/api/test-requests")
@RequiredArgsConstructor
@Tag(name = "Test Request Management", description = "Lab/Flight Test Requests (LTR/FTR)")
public class VvTestRequestController {

    private final VvoService vvoService;

    @PostMapping
    @Operation(summary = "Create Test Request")
    public ResponseEntity<TestRequestResponse> create(@Valid @RequestBody CreateTestRequestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vvoService.createTestRequest(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Test Request by ID")
    public ResponseEntity<TestRequestResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(vvoService.getTestRequest(id));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List Test Requests by project")
    public ResponseEntity<List<TestRequestResponse>> getByProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(vvoService.getTestRequestsByProject(projectId));
    }

    @GetMapping("/{id}/vvos")
    @Operation(summary = "Get VVOs contained in this Test Request")
    public ResponseEntity<List<VvoResponse>> getVvos(@PathVariable UUID id) {
        return ResponseEntity.ok(vvoService.getVvosForTestRequest(id));
    }
}
