package com.jira.report.controller;

import com.jira.report.service.StandardReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class StandardReportController {

    private final StandardReportService reportService;

    @GetMapping("/created-vs-resolved")
    public ResponseEntity<Map<String, Object>> getCreatedVsResolved(
            @RequestParam UUID projectId,
            @RequestParam(defaultValue = "MONTHLY") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(reportService.getCreatedVsResolved(projectId, period, startDate, endDate));
    }

    @GetMapping("/average-age")
    public ResponseEntity<Map<String, Object>> getAverageAge(
            @RequestParam UUID projectId,
            @RequestParam(required = false) String groupBy) {
        return ResponseEntity.ok(reportService.getAverageAge(projectId, groupBy));
    }

    @GetMapping("/resolution-time")
    public ResponseEntity<Map<String, Object>> getResolutionTime(
            @RequestParam UUID projectId,
            @RequestParam(defaultValue = "MONTHLY") String period) {
        return ResponseEntity.ok(reportService.getResolutionTime(projectId, period));
    }

    @GetMapping("/group-by")
    public ResponseEntity<Map<String, Object>> getGroupBy(
            @RequestParam UUID projectId,
            @RequestParam String field) {
        return ResponseEntity.ok(reportService.getGroupBy(projectId, field));
    }

    @GetMapping("/pie-chart")
    public ResponseEntity<Map<String, Object>> getPieChart(
            @RequestParam UUID projectId,
            @RequestParam String field) {
        return ResponseEntity.ok(reportService.getPieChart(projectId, field));
    }

    @GetMapping("/recently-created")
    public ResponseEntity<Map<String, Object>> getRecentlyCreated(
            @RequestParam UUID projectId,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "DAILY") String period) {
        return ResponseEntity.ok(reportService.getRecentlyCreated(projectId, days, period));
    }

    @GetMapping("/time-since")
    public ResponseEntity<Map<String, Object>> getTimeSince(
            @RequestParam UUID projectId,
            @RequestParam(defaultValue = "updatedAt") String field,
            @RequestParam(defaultValue = "30d") String olderThan) {
        return ResponseEntity.ok(reportService.getTimeSince(projectId, field, olderThan));
    }

    @GetMapping("/version-workload")
    public ResponseEntity<Map<String, Object>> getVersionWorkload(@RequestParam UUID versionId) {
        return ResponseEntity.ok(reportService.getVersionWorkload(versionId));
    }

    @GetMapping("/user-workload")
    public ResponseEntity<Map<String, Object>> getUserWorkload(@RequestParam UUID userId) {
        return ResponseEntity.ok(reportService.getUserWorkload(userId));
    }
}
