package com.jira.sprint.controller;

import com.jira.sprint.dto.*;
import com.jira.sprint.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
@Tag(name = "Dashboards", description = "Dashboard management API")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get dashboards", description = "Get all dashboards for a user")
    public ResponseEntity<List<DashboardResponse>> getDashboards(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestParam(defaultValue = "true") boolean includeGlobal) {
        return ResponseEntity.ok(dashboardService.getDashboards(userId, includeGlobal));
    }

    @GetMapping("/{dashboardId}")
    @Operation(summary = "Get dashboard", description = "Get a specific dashboard by ID")
    public ResponseEntity<DashboardResponse> getDashboard(@PathVariable UUID dashboardId) {
        DashboardResponse dashboard = dashboardService.getDashboard(dashboardId);
        return dashboard != null
                ? ResponseEntity.ok(dashboard)
                : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Create dashboard", description = "Create a new dashboard")
    public ResponseEntity<DashboardResponse> createDashboard(
            @RequestBody CreateDashboardRequest request,
            @RequestHeader(value = "X-User-Id") UUID userId) {
        return new ResponseEntity<>(
                dashboardService.createDashboard(userId, request),
                HttpStatus.CREATED);
    }

    @PutMapping("/{dashboardId}")
    @Operation(summary = "Update dashboard", description = "Update a dashboard")
    public ResponseEntity<DashboardResponse> updateDashboard(
            @PathVariable UUID dashboardId,
            @RequestBody CreateDashboardRequest request) {
        return ResponseEntity.ok(dashboardService.updateDashboard(dashboardId, request));
    }

    @DeleteMapping("/{dashboardId}")
    @Operation(summary = "Delete dashboard", description = "Delete a dashboard")
    public ResponseEntity<Void> deleteDashboard(@PathVariable UUID dashboardId) {
        dashboardService.deleteDashboard(dashboardId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{dashboardId}/gadgets")
    @Operation(summary = "Add gadget", description = "Add a gadget to a dashboard")
    public ResponseEntity<DashboardResponse> addGadget(
            @PathVariable UUID dashboardId,
            @RequestBody GadgetResponse gadget) {
        return new ResponseEntity<>(
                dashboardService.addGadget(dashboardId, gadget),
                HttpStatus.CREATED);
    }

    @PutMapping("/{dashboardId}/gadgets/{gadgetId}")
    @Operation(summary = "Update gadget", description = "Update a gadget's position or preferences")
    public ResponseEntity<DashboardResponse> updateGadget(
            @PathVariable UUID dashboardId,
            @PathVariable UUID gadgetId,
            @RequestBody GadgetResponse gadget) {
        return ResponseEntity.ok(dashboardService.updateGadget(dashboardId, gadgetId, gadget));
    }

    @DeleteMapping("/{dashboardId}/gadgets/{gadgetId}")
    @Operation(summary = "Remove gadget", description = "Remove a gadget from a dashboard")
    public ResponseEntity<DashboardResponse> removeGadget(
            @PathVariable UUID dashboardId,
            @PathVariable UUID gadgetId) {
        return ResponseEntity.ok(dashboardService.removeGadget(dashboardId, gadgetId));
    }

    @GetMapping("/gadgets/data")
    @Operation(summary = "Get gadget data", description = "Get data for a specific gadget type")
    public ResponseEntity<Map<String, Object>> getGadgetData(
            @RequestParam String gadgetType,
            @RequestParam(required = false) Map<String, Object> preferences) {
        return ResponseEntity.ok(dashboardService.getGadgetData(gadgetType, preferences));
    }
}