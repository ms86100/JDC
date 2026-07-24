package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.entity.VvoDefinition;
import com.jira.test.service.DoorsIntegrationService;
import com.jira.test.service.VvoBaselineService;
import com.jira.test.service.VvoTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/vvo/baseline")
@RequiredArgsConstructor
@Tag(name = "VVO Baselining", description = "VVO baseline management — tag, publish, and manage baselines")
public class VvoBaselineController {

    private final VvoBaselineService baselineService;
    private final DoorsIntegrationService doorsService;
    private final VvoTransferService transferService;

    @PostMapping("/tag")
    @Operation(summary = "Tag VVOs with a baseline Fix Version")
    public ResponseEntity<BulkOperationResponse> tagBaseline(
            @Valid @RequestBody BaselineTagRequest request) {
        return ResponseEntity.ok(baselineService.tagBaseline(
                request.getProjectId(), request.getFixVersionId(), request.getVvoIds()));
    }

    @PostMapping("/publish")
    @Operation(summary = "Publish baseline — transition all VERIFIED VVOs to RELEASED")
    public ResponseEntity<BulkOperationResponse> publishBaseline(
            @RequestParam UUID projectId,
            @RequestParam UUID fixVersionId) {
        return ResponseEntity.ok(baselineService.publishBaseline(projectId, fixVersionId));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get baseline summary with status counts")
    public ResponseEntity<BaselineSummaryResponse> getBaselineSummary(
            @RequestParam UUID projectId,
            @RequestParam UUID fixVersionId) {
        return ResponseEntity.ok(baselineService.getBaselineSummary(projectId, fixVersionId));
    }

    @PostMapping("/clone-with-supersede/{vvoId}")
    @Operation(summary = "Clone VVO with automatic supersede of original")
    public ResponseEntity<VvoResponse> cloneWithSupersede(@PathVariable UUID vvoId) {
        VvoDefinition cloned = baselineService.cloneWithSupersede(vvoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(VvoResponse.builder()
                .id(cloned.getId())
                .issueKey(cloned.getIssueKey())
                .vvoVersion(cloned.getVvoVersion())
                .summary(cloned.getSummary())
                .status(cloned.getStatus())
                .cloneSourceId(cloned.getCloneSourceId())
                .build());
    }

    @PostMapping("/doors/export")
    @Operation(summary = "Export VVOs to CSV for DOORS import")
    public ResponseEntity<String> exportForDoors(
            @Valid @RequestBody DoorsExportRequest request) {
        String csv = doorsService.exportVvosForDoors(request.getProjectId(), request.getFixVersionId());
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=vvo_export_doors.csv")
                .body(csv);
    }

    @PostMapping("/doors/import")
    @Operation(summary = "Import DOORS IDs from CSV into VVOs")
    public ResponseEntity<DoorsImportResponse> importDoorsIds(
            @RequestParam UUID projectId,
            @RequestBody String csvContent) {
        return ResponseEntity.ok(doorsService.importDoorsIds(projectId, csvContent));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer VVOs from DO project to LAB project")
    public ResponseEntity<VvoTransferResponse> transferVvos(
            @Valid @RequestBody VvoTransferRequest request) {
        return ResponseEntity.ok(transferService.transferVvos(
                request.getSourceProjectId(), request.getTargetProjectId(),
                request.getFixVersionId(), request.isPreviewOnly()));
    }
}
