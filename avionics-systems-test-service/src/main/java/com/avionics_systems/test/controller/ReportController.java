package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Test Reports", description = "APIs for test reporting and analytics")
public class ReportController {

    private final ReportingService reportingService;

    @GetMapping("/summary")
    @PreAuthorize("@projectSecurity.canViewReports(authentication, #projectId)")
    @Operation(summary = "Get test summary report for a project")
    public ResponseEntity<ReportSummaryResponse> getSummary(@RequestParam UUID projectId) {
        ReportSummaryResponse summary = reportingService.getSummary(projectId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/trend")
    @PreAuthorize("@projectSecurity.canViewReports(authentication, #projectId)")
    @Operation(summary = "Get test trend data over time")
    public ResponseEntity<List<ReportSummaryResponse.TestTrendPoint>> getTrend(
            @RequestParam UUID projectId,
            @RequestParam(defaultValue = "30") int days) {
        List<ReportSummaryResponse.TestTrendPoint> trend = reportingService.getTrend(projectId, days);
        return ResponseEntity.ok(trend);
    }

    @GetMapping("/coverage")
    @PreAuthorize("@projectSecurity.canViewReports(authentication, #projectId)")
    @Operation(summary = "Get test coverage report")
    public ResponseEntity<Map<String, Object>> getCoverage(@RequestParam UUID projectId) {
        Map<String, Object> coverage = reportingService.getCoverage(projectId);
        return ResponseEntity.ok(coverage);
    }

    @GetMapping("/defect-density")
    @PreAuthorize("@projectSecurity.canViewReports(authentication, #projectId)")
    @Operation(summary = "Get defect density metrics")
    public ResponseEntity<Map<String, Object>> getDefectDensity(@RequestParam UUID projectId) {
        Map<String, Object> density = reportingService.getDefectDensity(projectId);
        return ResponseEntity.ok(density);
    }
}