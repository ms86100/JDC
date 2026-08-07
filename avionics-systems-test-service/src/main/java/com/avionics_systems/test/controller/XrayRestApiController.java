package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.service.XrayApiMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/raven/1.0")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Xray REST API", description = "Xray-compatible REST API for test execution import and retrieval")
public class XrayRestApiController {

    private final XrayApiMappingService xrayApiMappingService;

    @PostMapping("/import/execution")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Import a test execution in Xray JSON format")
    public ResponseEntity<XrayImportResponse> importExecution(
            @RequestBody XrayImportExecutionRequest request,
            @RequestParam UUID projectId) {
        log.info("Received Xray execution import request for project: {}", projectId);
        XrayImportResponse response = xrayApiMappingService.importExecution(request, projectId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/test/{testKey}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get a test by its key in Xray format")
    public ResponseEntity<TestResponse> getTest(
            @PathVariable String testKey,
            @RequestParam UUID projectId) {
        log.info("Fetching test by key: {} for project: {}", testKey, projectId);
        TestResponse response = xrayApiMappingService.getTestByKey(testKey, projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/testexec/{testExecId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get a test execution summary")
    public ResponseEntity<Map<String, Object>> getTestExecution(
            @PathVariable UUID testExecId,
            @RequestParam UUID projectId) {
        log.info("Fetching test execution: {} for project: {}", testExecId, projectId);
        Map<String, Object> summary = xrayApiMappingService.getExecutionSummary(testExecId, projectId);
        return ResponseEntity.ok(summary);
    }
}
