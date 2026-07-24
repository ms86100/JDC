package com.jira.board.controller;

import com.jira.board.dto.*;
import com.jira.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
@Slf4j
public class BoardController {

    private final BoardService boardService;

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<AgileBoardResponse>> getBoardsByProject(@PathVariable UUID projectId) {
        log.info("Getting boards for project: {}", projectId);
        return ResponseEntity.ok(boardService.getBoardsByProject(projectId));
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<AgileBoardResponse> getBoard(@PathVariable UUID boardId) {
        log.info("Getting board: {}", boardId);
        return ResponseEntity.ok(boardService.getBoard(boardId));
    }

    @GetMapping("/{boardId}/data")
    public ResponseEntity<BoardDataResponse> getBoardData(@PathVariable UUID boardId) {
        log.info("Getting board data: {}", boardId);
        return ResponseEntity.ok(boardService.getBoardData(boardId));
    }

    @PostMapping
    public ResponseEntity<AgileBoardResponse> createBoard(@RequestBody CreateBoardRequest request) {
        log.info("Creating board: {}", request.getName());
        return ResponseEntity.ok(boardService.createBoard(request));
    }

    @PutMapping("/{boardId}")
    public ResponseEntity<AgileBoardResponse> updateBoard(
            @PathVariable UUID boardId,
            @RequestBody UpdateBoardRequest request) {
        log.info("Updating board: {}", boardId);
        return ResponseEntity.ok(boardService.updateBoard(boardId, request));
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(@PathVariable UUID boardId) {
        log.info("Deleting board: {}", boardId);
        boardService.deleteBoard(boardId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{boardId}/copy")
    public ResponseEntity<AgileBoardResponse> copyBoard(
            @PathVariable UUID boardId,
            @RequestBody(required = false) CopyBoardRequest request) {
        log.info("Copying board: {}", boardId);
        String newName = request != null ? request.getName() : null;
        return ResponseEntity.ok(boardService.copyBoard(boardId, newName));
    }

    @GetMapping("/{boardId}/issues")
    public ResponseEntity<List<BoardIssueResponse>> getBoardIssues(
            @PathVariable UUID boardId,
            @RequestParam(required = false) String jql) {
        log.info("Getting issues for board: {} with JQL: {}", boardId, jql);
        return ResponseEntity.ok(boardService.getBoardIssues(boardId, jql));
    }

    @PostMapping("/{boardId}/quick-filter/{filterId}")
    public ResponseEntity<List<BoardIssueResponse>> applyQuickFilter(
            @PathVariable UUID boardId,
            @PathVariable String filterId) {
        log.info("Applying quick filter {} to board {}", filterId, boardId);
        return ResponseEntity.ok(boardService.applyQuickFilter(boardId, filterId));
    }

    @PutMapping("/{boardId}/issues/{issueId}/move")
    public ResponseEntity<BoardIssueResponse> moveIssue(
            @PathVariable UUID boardId,
            @PathVariable UUID issueId,
            @RequestBody MoveIssueRequest request) {
        log.info("Moving issue {} to status {} on board {}", issueId, request.getStatus(), boardId);
        return ResponseEntity.ok(boardService.moveIssue(boardId, issueId, request.getStatus(), request.getRank()));
    }

    @PostMapping("/{boardId}/issues/{issueId}/reorder")
    public ResponseEntity<Void> reorderIssue(
            @PathVariable UUID boardId,
            @PathVariable UUID issueId,
            @RequestBody ReorderIssueRequest request) {
        log.info("Reordering issue {} to position {} in status {}", issueId, request.getIndex(), request.getStatus());
        boardService.reorderIssue(boardId, issueId, request.getIndex(), request.getStatus());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{boardId}/swimlanes")
    public ResponseEntity<SwimlaneDataResponse> getSwimlaneData(
            @PathVariable UUID boardId,
            @RequestParam String field) {
        log.info("Getting swimlane data for board: {} field: {}", boardId, field);
        return ResponseEntity.ok(boardService.getSwimlaneData(boardId, field));
    }

    @GetMapping("/{boardId}/velocity")
    public ResponseEntity<VelocityResponse> getVelocity(@PathVariable UUID boardId) {
        log.info("Getting velocity for board: {}", boardId);
        return ResponseEntity.ok(boardService.getVelocity(boardId));
    }

    @GetMapping("/{boardId}/sprints/{sprintId}/capacity")
    public ResponseEntity<CapacityResponse> getSprintCapacity(
            @PathVariable UUID boardId,
            @PathVariable UUID sprintId) {
        log.info("Getting capacity for sprint: {} on board: {}", sprintId, boardId);
        return ResponseEntity.ok(boardService.getSprintCapacity(boardId, sprintId));
    }

    @GetMapping("/{boardId}/config")
    public ResponseEntity<BoardConfigResponse> getBoardConfig(@PathVariable UUID boardId) {
        log.info("Getting config for board: {}", boardId);
        return ResponseEntity.ok(boardService.getBoardConfig(boardId));
    }

    @PutMapping("/{boardId}/config")
    public ResponseEntity<BoardConfigResponse> updateBoardConfig(
            @PathVariable UUID boardId,
            @RequestBody UpdateBoardConfigRequest request) {
        log.info("Updating config for board: {}", boardId);
        return ResponseEntity.ok(boardService.updateBoardConfig(boardId, request));
    }

    @PutMapping("/{boardId}/columns/{columnId}")
    public ResponseEntity<BoardColumnResponse> updateColumn(
            @PathVariable UUID boardId,
            @PathVariable UUID columnId,
            @RequestBody BoardColumnResponse request) {
        log.info("Updating column {} on board {}", columnId, boardId);
        return ResponseEntity.ok(boardService.updateColumn(boardId, columnId, request));
    }
}