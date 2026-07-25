package com.jira.dashboard.controller;

import com.jira.dashboard.dto.*;
import com.jira.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
@Tag(name = "Dashboards", description = "Dashboard and Gadget management endpoints")
public class DashboardController {

    private final DashboardService dashboardService;
    private final MessageSource messageSource;

    @PostMapping
    @Operation(summary = "Create a new dashboard", description = "Creates a new dashboard for the current user")
    public ResponseEntity<DashboardResponse> createDashboard(
            @Valid @RequestBody CreateDashboardRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) { throw new IllegalArgumentException(messageSource.getMessage("error.header.user-id.required", null, Locale.ENGLISH)); }
        UUID actor = userId;
        DashboardResponse response = dashboardService.createDashboard(request, actor);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get user dashboards", description = "Returns paginated list of user's dashboards")
    public ResponseEntity<Page<DashboardResponse>> getMyDashboards(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (userId == null) { throw new IllegalArgumentException(messageSource.getMessage("error.header.user-id.required", null, Locale.ENGLISH)); }
        UUID actor = userId;
        return ResponseEntity.ok(dashboardService.getDashboardsByOwner(actor, PageRequest.of(page, size)));
    }

    @GetMapping("/accessible")
    @Operation(summary = "Get accessible dashboards", description = "Returns all dashboards the user can access")
    public ResponseEntity<List<DashboardResponse>> getAccessibleDashboards(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) { throw new IllegalArgumentException(messageSource.getMessage("error.header.user-id.required", null, Locale.ENGLISH)); }
        UUID actor = userId;
        return ResponseEntity.ok(dashboardService.getAccessibleDashboards(actor));
    }

    @GetMapping("/favorites")
    @Operation(summary = "Get favorite dashboards", description = "Returns user's favorite dashboards")
    public ResponseEntity<List<DashboardResponse>> getFavoriteDashboards(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) { throw new IllegalArgumentException(messageSource.getMessage("error.header.user-id.required", null, Locale.ENGLISH)); }
        UUID actor = userId;
        return ResponseEntity.ok(dashboardService.getFavoriteDashboards(actor));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dashboard by ID", description = "Returns dashboard details with gadgets")
    public ResponseEntity<DashboardResponse> getDashboard(
            @Parameter(description = "Dashboard ID") @PathVariable UUID id) {
        return ResponseEntity.ok(dashboardService.getDashboard(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update dashboard", description = "Updates dashboard configuration")
    public ResponseEntity<DashboardResponse> updateDashboard(
            @Parameter(description = "Dashboard ID") @PathVariable UUID id,
            @RequestBody UpdateDashboardRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) { throw new IllegalArgumentException(messageSource.getMessage("error.header.user-id.required", null, Locale.ENGLISH)); }
        UUID actor = userId;
        return ResponseEntity.ok(dashboardService.updateDashboard(id, request, actor));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete dashboard", description = "Deletes a dashboard and all its gadgets")
    public ResponseEntity<Void> deleteDashboard(
            @Parameter(description = "Dashboard ID") @PathVariable UUID id) {
        dashboardService.deleteDashboard(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/share")
    @Operation(summary = "Share dashboard", description = "Shares dashboard with users, groups, or projects")
    public ResponseEntity<DashboardResponse> shareDashboard(
            @Parameter(description = "Dashboard ID") @PathVariable UUID id,
            @Valid @RequestBody ShareDashboardRequest request) {
        return ResponseEntity.ok(dashboardService.shareDashboard(id, request));
    }

    @PostMapping("/{id}/favorite")
    @Operation(summary = "Toggle favorite", description = "Toggles dashboard favorite status")
    public ResponseEntity<DashboardResponse> toggleFavorite(
            @Parameter(description = "Dashboard ID") @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) { throw new IllegalArgumentException(messageSource.getMessage("error.header.user-id.required", null, Locale.ENGLISH)); }
        UUID actor = userId;
        return ResponseEntity.ok(dashboardService.toggleFavorite(id, actor));
    }

    @GetMapping("/gadgets")
    @Operation(summary = "Get available gadgets", description = "Returns all available gadgets")
    public ResponseEntity<List<GadgetResponse>> getGadgets(
            @Parameter(description = "Filter by category") @RequestParam(required = false) String category) {
        return ResponseEntity.ok(dashboardService.getAvailableGadgets(category));
    }

    @GetMapping("/{id}/gadgets")
    @Operation(summary = "Get dashboard gadgets", description = "Returns all gadgets on a dashboard")
    public ResponseEntity<List<GadgetInstanceResponse>> getDashboardGadgets(
            @Parameter(description = "Dashboard ID") @PathVariable UUID id) {
        return ResponseEntity.ok(dashboardService.getDashboardGadgets(id));
    }

    @PostMapping("/{id}/gadgets")
    @Operation(summary = "Add gadget to dashboard", description = "Adds a gadget instance to a dashboard")
    public ResponseEntity<GadgetInstanceResponse> addGadgetToDashboard(
            @Parameter(description = "Dashboard ID") @PathVariable UUID id,
            @Valid @RequestBody CreateGadgetInstanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dashboardService.addGadgetToDashboard(id, request));
    }

    @DeleteMapping("/{dashboardId}/gadgets/{gadgetInstanceId}")
    @Operation(summary = "Remove gadget from dashboard", description = "Removes a gadget instance from a dashboard")
    public ResponseEntity<Void> removeGadget(
            @Parameter(description = "Dashboard ID") @PathVariable UUID dashboardId,
            @Parameter(description = "Gadget Instance ID") @PathVariable UUID gadgetInstanceId) {
        dashboardService.removeGadgetFromDashboard(dashboardId, gadgetInstanceId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/gadgets/{gadgetInstanceId}")
    @Operation(summary = "Update gadget instance", description = "Updates gadget instance configuration")
    public ResponseEntity<GadgetInstanceResponse> updateGadgetInstance(
            @Parameter(description = "Gadget Instance ID") @PathVariable UUID gadgetInstanceId,
            @RequestParam(required = false) String config,
            @RequestParam(required = false) String filters) {
        return ResponseEntity.ok(dashboardService.updateGadgetInstance(gadgetInstanceId, config, filters));
    }

    @PostMapping("/gadgets/{gadgetInstanceId}/minimize")
    @Operation(summary = "Toggle gadget minimize", description = "Toggles gadget minimized state")
    public ResponseEntity<GadgetInstanceResponse> toggleGadgetMinimize(
            @Parameter(description = "Gadget Instance ID") @PathVariable UUID gadgetInstanceId) {
        return ResponseEntity.ok(dashboardService.toggleGadgetMinimized(gadgetInstanceId));
    }
}