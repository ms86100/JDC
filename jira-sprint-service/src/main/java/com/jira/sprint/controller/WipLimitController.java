package com.jira.sprint.controller;

import com.jira.sprint.dto.UpdateWipLimitRequest;
import com.jira.sprint.dto.WipLimitResponse;
import com.jira.sprint.service.WipLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for WIP (Work In Progress) limit management.
 * Provides endpoints for configuring and monitoring WIP limits on Kanban boards.
 */
@RestController
@RequestMapping("/api/boards/{boardId}/wip-limits")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WIP Limits", description = "WIP limit management for Kanban boards")
@CrossOrigin(origins = "*")
public class WipLimitController {

    private final WipLimitService wipLimitService;

    @GetMapping
    @Operation(summary = "Get all WIP limits for a board")
    public ResponseEntity<List<WipLimitResponse>> getBoardWipLimits(@PathVariable UUID boardId) {
        log.info("Getting WIP limits for board: {}", boardId);
        return ResponseEntity.ok(wipLimitService.getBoardWipLimits(boardId));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get WIP limits summary for a board")
    public ResponseEntity<Map<String, Object>> getWipLimitsSummary(@PathVariable UUID boardId) {
        log.info("Getting WIP limits summary for board: {}", boardId);
        return ResponseEntity.ok(wipLimitService.getWipLimitsSummary(boardId));
    }

    @GetMapping("/exceeded")
    @Operation(summary = "Get columns exceeding WIP limits")
    public ResponseEntity<List<WipLimitResponse>> getExceededLimits(@PathVariable UUID boardId) {
        log.info("Getting columns exceeding WIP limits for board: {}", boardId);
        return ResponseEntity.ok(wipLimitService.getColumnsExceedingLimits(boardId));
    }

    @GetMapping("/{columnId}")
    @Operation(summary = "Get WIP limit for a specific column")
    public ResponseEntity<WipLimitResponse> getColumnWipLimit(
            @PathVariable UUID boardId,
            @PathVariable UUID columnId) {
        log.info("Getting WIP limit for column {} on board {}", columnId, boardId);
        return ResponseEntity.ok(wipLimitService.getColumnWipLimit(columnId));
    }

    @PutMapping
    @Operation(summary = "Update WIP limit for a column")
    public ResponseEntity<WipLimitResponse> updateWipLimit(
            @PathVariable UUID boardId,
            @Valid @RequestBody UpdateWipLimitRequest request) {
        log.info("Updating WIP limit on board {}: column={}, limit={}",
                boardId, request.getColumnId(), request.getWipLimit());
        return ResponseEntity.ok(wipLimitService.updateWipLimit(boardId, request));
    }

    @GetMapping("/{columnId}/can-add")
    @Operation(summary = "Check if an issue can be added to column without exceeding limit")
    public ResponseEntity<Map<String, Boolean>> canAddIssue(@PathVariable UUID columnId) {
        boolean canAdd = wipLimitService.canAddIssue(columnId);
        return ResponseEntity.ok(Map.of("canAdd", canAdd));
    }
}