package com.jira.plan.controller;

import com.jira.plan.dto.request.*;
import com.jira.plan.dto.response.*;
import com.jira.plan.service.BoardConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Board/RapidView configuration.
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class BoardConfigController {

    private final BoardConfigService boardConfigService;

    // Board CRUD

    @GetMapping("/{planId}/boards")
    public ResponseEntity<List<BoardConfigResponse>> getBoards(@PathVariable UUID planId) {
        return ResponseEntity.ok(boardConfigService.getBoardsByPlanId(planId));
    }

    @GetMapping("/boards/{boardId}")
    public ResponseEntity<BoardConfigResponse> getBoard(@PathVariable UUID boardId) {
        return ResponseEntity.ok(boardConfigService.getBoardById(boardId));
    }

    @PostMapping("/{planId}/boards")
    public ResponseEntity<BoardConfigResponse> createBoard(
            @PathVariable UUID planId,
            @RequestBody CreateBoardConfigRequest request) {
        return ResponseEntity.ok(boardConfigService.createBoard(planId, request));
    }

    @PutMapping("/boards/{boardId}")
    public ResponseEntity<BoardConfigResponse> updateBoard(
            @PathVariable UUID boardId,
            @RequestBody CreateBoardConfigRequest request) {
        return ResponseEntity.ok(boardConfigService.updateBoard(boardId, request));
    }

    @DeleteMapping("/boards/{boardId}")
    public ResponseEntity<Void> deleteBoard(@PathVariable UUID boardId) {
        boardConfigService.deleteBoard(boardId);
        return ResponseEntity.noContent().build();
    }

    // Columns

    @PostMapping("/boards/{boardId}/columns")
    public ResponseEntity<BoardColumnResponse> addColumn(
            @PathVariable UUID boardId,
            @RequestBody CreateBoardColumnRequest request) {
        return ResponseEntity.ok(boardConfigService.addColumn(boardId, request));
    }

    @PutMapping("/boards/columns/{columnId}")
    public ResponseEntity<BoardColumnResponse> updateColumn(
            @PathVariable UUID columnId,
            @RequestBody CreateBoardColumnRequest request) {
        return ResponseEntity.ok(boardConfigService.updateColumn(columnId, request));
    }

    @DeleteMapping("/boards/columns/{columnId}")
    public ResponseEntity<Void> deleteColumn(@PathVariable UUID columnId) {
        boardConfigService.deleteColumn(columnId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/boards/{boardId}/columns")
    public ResponseEntity<Void> updateColumnsOrder(
            @PathVariable UUID boardId,
            @RequestBody List<UUID> columnIds) {
        boardConfigService.updateColumnsOrder(boardId, columnIds);
        return ResponseEntity.ok().build();
    }

    // Quick Filters

    @PostMapping("/boards/{boardId}/quick-filters")
    public ResponseEntity<BoardQuickFilterResponse> addQuickFilter(
            @PathVariable UUID boardId,
            @RequestBody CreateBoardQuickFilterRequest request) {
        return ResponseEntity.ok(boardConfigService.addQuickFilter(boardId, request));
    }

    @DeleteMapping("/boards/quick-filters/{filterId}")
    public ResponseEntity<Void> deleteQuickFilter(@PathVariable UUID filterId) {
        boardConfigService.deleteQuickFilter(filterId);
        return ResponseEntity.noContent().build();
    }

    // Swimlanes

    @PostMapping("/boards/{boardId}/swimlanes")
    public ResponseEntity<BoardSwimlaneResponse> addSwimlane(
            @PathVariable UUID boardId,
            @RequestBody CreateBoardSwimlaneRequest request) {
        return ResponseEntity.ok(boardConfigService.addSwimlane(boardId, request));
    }

    @DeleteMapping("/boards/swimlanes/{swimlaneId}")
    public ResponseEntity<Void> deleteSwimlane(@PathVariable UUID swimlaneId) {
        boardConfigService.deleteSwimlane(swimlaneId);
        return ResponseEntity.noContent().build();
    }

    // Card Colors

    @PostMapping("/boards/{boardId}/card-colors")
    public ResponseEntity<BoardCardColorResponse> addCardColor(
            @PathVariable UUID boardId,
            @RequestBody CreateBoardCardColorRequest request) {
        return ResponseEntity.ok(boardConfigService.addCardColor(boardId, request));
    }

    @DeleteMapping("/boards/card-colors/{colorId}")
    public ResponseEntity<Void> deleteCardColor(@PathVariable UUID colorId) {
        boardConfigService.deleteCardColor(colorId);
        return ResponseEntity.noContent().build();
    }

    // Detail Fields

    @PostMapping("/boards/{boardId}/detail-fields")
    public ResponseEntity<BoardDetailFieldResponse> addDetailField(
            @PathVariable UUID boardId,
            @RequestBody CreateBoardDetailFieldRequest request) {
        return ResponseEntity.ok(boardConfigService.addDetailField(boardId, request));
    }

    @DeleteMapping("/boards/detail-fields/{fieldId}")
    public ResponseEntity<Void> deleteDetailField(@PathVariable UUID fieldId) {
        boardConfigService.deleteDetailField(fieldId);
        return ResponseEntity.noContent().build();
    }
}