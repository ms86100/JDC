package com.jira.board.controller;

import com.jira.board.entity.*;
import com.jira.board.service.BoardConfigurationService;
import com.jira.board.service.ControlChartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
@Slf4j
public class BoardConfigurationController {

    private final BoardConfigurationService configService;
    private final ControlChartService controlChartService;

    private void requireBoardAdmin(UUID boardId, UUID userId) {
        if (userId == null) return;
        if (!configService.isAdministrator(boardId, userId)) {
            throw new RuntimeException("User " + userId + " is not an administrator of board " + boardId);
        }
    }

    // === Administrators ===

    @GetMapping("/{boardId}/administrators")
    public ResponseEntity<List<BoardAdministrator>> getAdministrators(@PathVariable UUID boardId) {
        return ResponseEntity.ok(configService.getAdministrators(boardId));
    }

    @PostMapping("/{boardId}/administrators")
    public ResponseEntity<BoardAdministrator> addAdministrator(
            @PathVariable UUID boardId, @RequestBody Map<String, String> request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        requireBoardAdmin(boardId, userId);
        UUID holderId = UUID.fromString(request.get("holderId"));
        String holderType = request.getOrDefault("holderType", "USER");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configService.addAdministrator(boardId, holderId, holderType));
    }

    @DeleteMapping("/{boardId}/administrators/{holderId}")
    public ResponseEntity<Void> removeAdministrator(
            @PathVariable UUID boardId, @PathVariable UUID holderId) {
        configService.removeAdministrator(boardId, holderId);
        return ResponseEntity.noContent().build();
    }

    // === Swimlanes ===

    @GetMapping("/{boardId}/swimlanes/config")
    public ResponseEntity<List<BoardSwimlane>> getSwimlanes(@PathVariable UUID boardId) {
        return ResponseEntity.ok(configService.getSwimlanes(boardId));
    }

    @PostMapping("/{boardId}/swimlanes/config")
    public ResponseEntity<BoardSwimlane> createSwimlane(
            @PathVariable UUID boardId, @RequestBody BoardSwimlane swimlane) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configService.createSwimlane(boardId, swimlane));
    }

    @PutMapping("/swimlanes/{swimlaneId}")
    public ResponseEntity<BoardSwimlane> updateSwimlane(
            @PathVariable UUID swimlaneId, @RequestBody BoardSwimlane update) {
        return ResponseEntity.ok(configService.updateSwimlane(swimlaneId, update));
    }

    @DeleteMapping("/swimlanes/{swimlaneId}")
    public ResponseEntity<Void> deleteSwimlane(@PathVariable UUID swimlaneId) {
        configService.deleteSwimlane(swimlaneId);
        return ResponseEntity.noContent().build();
    }

    // === Card Color Rules ===

    @GetMapping("/{boardId}/card-colors")
    public ResponseEntity<List<BoardCardColorRule>> getCardColorRules(@PathVariable UUID boardId) {
        return ResponseEntity.ok(configService.getCardColorRules(boardId));
    }

    @PostMapping("/{boardId}/card-colors")
    public ResponseEntity<BoardCardColorRule> createCardColorRule(
            @PathVariable UUID boardId, @RequestBody BoardCardColorRule rule) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configService.createCardColorRule(boardId, rule));
    }

    @PutMapping("/card-colors/{ruleId}")
    public ResponseEntity<BoardCardColorRule> updateCardColorRule(
            @PathVariable UUID ruleId, @RequestBody BoardCardColorRule update) {
        return ResponseEntity.ok(configService.updateCardColorRule(ruleId, update));
    }

    @DeleteMapping("/card-colors/{ruleId}")
    public ResponseEntity<Void> deleteCardColorRule(@PathVariable UUID ruleId) {
        configService.deleteCardColorRule(ruleId);
        return ResponseEntity.noContent().build();
    }

    // === Card Fields ===

    @GetMapping("/{boardId}/card-fields")
    public ResponseEntity<List<BoardCardField>> getCardFields(@PathVariable UUID boardId) {
        return ResponseEntity.ok(configService.getCardFields(boardId));
    }

    @PostMapping("/{boardId}/card-fields")
    public ResponseEntity<BoardCardField> addCardField(
            @PathVariable UUID boardId, @RequestBody BoardCardField field) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configService.addCardField(boardId, field));
    }

    @PutMapping("/{boardId}/card-fields")
    public ResponseEntity<Void> replaceCardFields(
            @PathVariable UUID boardId, @RequestBody List<BoardCardField> fields) {
        configService.replaceCardFields(boardId, fields);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/card-fields/{fieldId}")
    public ResponseEntity<Void> removeCardField(@PathVariable UUID fieldId) {
        configService.removeCardField(fieldId);
        return ResponseEntity.noContent().build();
    }

    // === Issue Detail View Fields ===

    @GetMapping("/{boardId}/issue-detail-fields")
    public ResponseEntity<List<BoardIssueDetailField>> getIssueDetailFields(@PathVariable UUID boardId) {
        return ResponseEntity.ok(configService.getIssueDetailFields(boardId));
    }

    @PostMapping("/{boardId}/issue-detail-fields")
    public ResponseEntity<BoardIssueDetailField> addIssueDetailField(
            @PathVariable UUID boardId, @RequestBody BoardIssueDetailField field) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configService.addIssueDetailField(boardId, field));
    }

    @PutMapping("/{boardId}/issue-detail-fields")
    public ResponseEntity<Void> replaceIssueDetailFields(
            @PathVariable UUID boardId, @RequestBody List<BoardIssueDetailField> fields) {
        configService.replaceIssueDetailFields(boardId, fields);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/issue-detail-fields/{fieldId}")
    public ResponseEntity<Void> removeIssueDetailField(@PathVariable UUID fieldId) {
        configService.removeIssueDetailField(fieldId);
        return ResponseEntity.noContent().build();
    }

    // === Cumulative Flow Diagram ===

    @GetMapping("/{boardId}/reports/cumulative-flow")
    public ResponseEntity<List<BoardCFDSnapshot>> getCumulativeFlowData(
            @PathVariable UUID boardId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(configService.getCFDData(boardId, startDate, endDate));
    }

    // === Kanban Backlog ===

    @GetMapping("/{boardId}/kanban-backlog")
    public ResponseEntity<Map<String, Object>> getKanbanBacklog(@PathVariable UUID boardId) {
        log.info("Getting kanban backlog for board {}", boardId);
        return ResponseEntity.ok(Map.of("boardId", boardId, "backlog", List.of(), "selectedForDevelopment", List.of()));
    }

    // === Control Chart ===

    @GetMapping("/{boardId}/reports/control-chart")
    public ResponseEntity<Map<String, Object>> getControlChart(
            @PathVariable UUID boardId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID projectId) {
        UUID effectiveProjectId = projectId != null ? projectId : boardId;
        return ResponseEntity.ok(controlChartService.getControlChart(boardId, effectiveProjectId, startDate, endDate));
    }

    // === Board Filter ===

    @PutMapping("/{boardId}/filter")
    public ResponseEntity<Map<String, Object>> updateBoardFilter(
            @PathVariable UUID boardId, @RequestBody Map<String, String> request) {
        String filterId = request.get("filterId");
        String jqlQuery = request.get("jqlQuery");
        log.info("Updating filter for board {}: filterId={}, jql={}", boardId, filterId, jqlQuery);
        return ResponseEntity.ok(Map.of("boardId", boardId.toString(), "status", "updated"));
    }

    // === Filter Subscriptions ===

    @GetMapping("/filters/{filterId}/subscriptions")
    public ResponseEntity<List<FilterSubscription>> getFilterSubscriptions(@PathVariable UUID filterId) {
        return ResponseEntity.ok(configService.getFilterSubscriptions(filterId));
    }

    @PostMapping("/filters/{filterId}/subscribe")
    public ResponseEntity<FilterSubscription> subscribe(
            @PathVariable UUID filterId, @RequestBody Map<String, String> request) {
        UUID userId = UUID.fromString(request.get("userId"));
        String frequency = request.getOrDefault("frequency", "DAILY");
        String emailAddress = request.get("emailAddress");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configService.subscribe(filterId, userId, frequency, emailAddress));
    }

    @DeleteMapping("/filters/subscriptions/{subscriptionId}")
    public ResponseEntity<Void> unsubscribe(@PathVariable UUID subscriptionId) {
        configService.unsubscribe(subscriptionId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/filters/subscriptions/{subscriptionId}/toggle")
    public ResponseEntity<FilterSubscription> toggleSubscription(
            @PathVariable UUID subscriptionId, @RequestParam boolean enabled) {
        return ResponseEntity.ok(configService.toggleSubscription(subscriptionId, enabled));
    }
}
