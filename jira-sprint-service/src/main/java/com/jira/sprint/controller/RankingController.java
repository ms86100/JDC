package com.jira.sprint.controller;

import com.jira.board.dto.BoardIssueResponse;
import com.jira.sprint.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for issue ranking operations using LexoRank.
 * Provides endpoints for moving, reordering, and rebalancing issue ranks.
 */
@RestController
@RequestMapping("/api/boards/{boardId}/ranking")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Issue Ranking", description = "LexoRank-based issue ranking operations")
public class RankingController {

    private final RankingService rankingService;

    @PostMapping("/move")
    @Operation(summary = "Move issue to specific position in a column")
    public ResponseEntity<MoveIssueResponse> moveIssue(
            @PathVariable UUID boardId,
            @RequestBody MoveIssueRequest request) {
        log.info("Moving issue {} to index {} in status {} on board {}",
                request.getIssueId(), request.getTargetIndex(), request.getStatus(), boardId);

        String newRank = rankingService.moveIssue(boardId, request.getIssueId(),
                request.getStatus(), request.getTargetIndex());

        return ResponseEntity.ok(new MoveIssueResponse(boardId, request.getIssueId(), newRank, request.getStatus()));
    }

    @PostMapping("/move-to-top")
    @Operation(summary = "Move issue to top of column")
    public ResponseEntity<MoveIssueResponse> moveIssueToTop(
            @PathVariable UUID boardId,
            @RequestBody MoveIssueToPositionRequest request) {
        log.info("Moving issue {} to top of column {} on board {}",
                request.getIssueId(), request.getStatus(), boardId);

        String newRank = rankingService.moveIssueToTop(boardId, request.getIssueId(), request.getStatus());

        return ResponseEntity.ok(new MoveIssueResponse(boardId, request.getIssueId(), newRank, request.getStatus()));
    }

    @PostMapping("/move-to-bottom")
    @Operation(summary = "Move issue to bottom of column")
    public ResponseEntity<MoveIssueResponse> moveIssueToBottom(
            @PathVariable UUID boardId,
            @RequestBody MoveIssueToPositionRequest request) {
        log.info("Moving issue {} to bottom of column {} on board {}",
                request.getIssueId(), request.getStatus(), boardId);

        String newRank = rankingService.moveIssueToBottom(boardId, request.getIssueId(), request.getStatus());

        return ResponseEntity.ok(new MoveIssueResponse(boardId, request.getIssueId(), newRank, request.getStatus()));
    }

    @PostMapping("/move-after")
    @Operation(summary = "Move issue after another issue in the same column")
    public ResponseEntity<MoveIssueResponse> moveIssueAfter(
            @PathVariable UUID boardId,
            @RequestBody MoveIssueAfterRequest request) {
        log.info("Moving issue {} after {} in column {} on board {}",
                request.getIssueId(), request.getAfterIssueId(), request.getStatus(), boardId);

        String newRank = rankingService.moveIssueAfter(boardId, request.getIssueId(),
                request.getAfterIssueId(), request.getStatus());

        return ResponseEntity.ok(new MoveIssueResponse(boardId, request.getIssueId(), newRank, request.getStatus()));
    }

    @PostMapping("/rebalance")
    @Operation(summary = "Rebalance ranks in a column")
    public ResponseEntity<Void> rebalanceColumn(
            @PathVariable UUID boardId,
            @RequestParam String status) {
        log.info("Rebalancing column {} on board {}", status, boardId);
        rankingService.rebalanceColumn(boardId, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/column")
    @Operation(summary = "Get sorted issues in a column")
    public ResponseEntity<List<BoardIssueResponse>> getSortedIssues(
            @PathVariable UUID boardId,
            @RequestParam String status) {
        log.info("Getting sorted issues for column {} on board {}", status, boardId);
        return ResponseEntity.ok(rankingService.getSortedIssues(boardId, status));
    }

    @PostMapping("/initialize")
    @Operation(summary = "Initialize ranks for unranked issues")
    public ResponseEntity<Void> initializeRanks(@PathVariable UUID boardId) {
        log.info("Initializing ranks for board {}", boardId);
        rankingService.initializeRanks(boardId);
        return ResponseEntity.ok().build();
    }

    // Request/Response DTOs
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MoveIssueRequest {
        private UUID issueId;
        private String status;
        private Integer targetIndex;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MoveIssueToPositionRequest {
        private UUID issueId;
        private String status;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MoveIssueAfterRequest {
        private UUID issueId;
        private UUID afterIssueId;
        private String status;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class MoveIssueResponse {
        private UUID boardId;
        private UUID issueId;
        private String newRank;
        private String status;
    }
}