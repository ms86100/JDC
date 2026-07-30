package com.avionics_systems.migration.controller;

import com.avionics_systems.migration.dto.*;
import com.avionics_systems.migration.service.field.BoardCardLayoutService;
import com.avionics_systems.migration.service.field.DashboardGadgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fields")
@RequiredArgsConstructor
@Tag(name = "Board & Dashboard Fields", description = "Phase 7 board card layout and Phase 9 dashboard gadgets")
public class FieldBoardDashboardController {

    private final BoardCardLayoutService boardCardLayoutService;
    private final DashboardGadgetService dashboardGadgetService;

    @GetMapping("/boards/{boardId}/card-layout")
    @Operation(summary = "Get board card layout and eligible custom fields")
    public ResponseEntity<BoardCardLayoutResponse> getBoardCardLayout(
            @PathVariable UUID boardId,
            @RequestParam(required = false) UUID projectId) {
        return ResponseEntity.ok(boardCardLayoutService.getCardLayout(boardId, projectId));
    }

    @PutMapping("/boards/{boardId}/card-layout")
    @Operation(summary = "Save board card layout (admin-selected fields only)")
    public ResponseEntity<BoardCardLayoutResponse> saveBoardCardLayout(
            @PathVariable UUID boardId,
            @RequestBody SaveBoardCardLayoutRequest request) {
        return ResponseEntity.ok(boardCardLayoutService.saveCardLayout(boardId, request));
    }

    @PostMapping("/boards/issues/visible-batch")
    @Operation(summary = "Batch fetch custom field values for board cards")
    public ResponseEntity<IssueFieldValuesBatchResponse> batchIssueFieldValues(
            @RequestBody IssueFieldValuesBatchRequest request) {
        return ResponseEntity.ok(boardCardLayoutService.batchCardFieldValues(request));
    }

    @GetMapping("/dashboard/gadgets")
    @Operation(summary = "List supported dashboard gadget types")
    public ResponseEntity<List<String>> listGadgets() {
        return ResponseEntity.ok(dashboardGadgetService.listSupportedGadgets());
    }

    @GetMapping("/dashboard/gadgets/{gadgetKey}")
    @Operation(summary = "Get dashboard gadget field configuration and statistics")
    public ResponseEntity<DashboardGadgetConfigResponse> getDashboardGadget(
            @PathVariable String gadgetKey,
            @RequestParam(defaultValue = "system") String dashboardKey,
            @RequestParam(required = false) UUID projectId) {
        return ResponseEntity.ok(dashboardGadgetService.getGadgetConfig(dashboardKey, gadgetKey, projectId));
    }

    @PutMapping("/dashboard/gadgets/{gadgetKey}")
    @Operation(summary = "Save dashboard gadget field configuration")
    public ResponseEntity<DashboardGadgetConfigResponse> saveDashboardGadget(
            @PathVariable String gadgetKey,
            @RequestBody SaveDashboardGadgetRequest request,
            @RequestParam(required = false) UUID projectId) {
        request.setGadgetKey(gadgetKey);
        return ResponseEntity.ok(dashboardGadgetService.saveGadgetConfig(request, projectId));
    }
}
