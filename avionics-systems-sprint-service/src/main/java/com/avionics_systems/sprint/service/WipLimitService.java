package com.avionics_systems.sprint.service;

import com.avionics_systems.board.dto.BoardIssueResponse;
import com.avionics_systems.board.entity.BoardColumn;
import com.avionics_systems.board.exception.ResourceNotFoundException;
import com.avionics_systems.board.repository.BoardColumnRepository;
import com.avionics_systems.cluster.util.StatusCategoryHelper;
import com.avionics_systems.sprint.dto.UpdateWipLimitRequest;
import com.avionics_systems.sprint.dto.WipLimitResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing WIP (Work In Progress) limits on Kanban boards.
 * Enforces limits on column issue counts to optimize flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WipLimitService {

    private final BoardColumnRepository columnRepository;
    private final IssueServiceClient issueServiceClient;
    private final MessageSource messageSource;

    /**
     * Get WIP limit status for all columns on a board.
     */
    @Transactional(readOnly = true)
    public List<WipLimitResponse> getBoardWipLimits(UUID boardId) {
        List<BoardColumn> columns = columnRepository.findByBoardIdOrderBySequenceAsc(boardId);
        List<BoardIssueResponse> issues = issueServiceClient.fetchBoardIssues(boardId, null);

        return columns.stream()
                .map(column -> createWipLimitResponse(column, issues))
                .collect(Collectors.toList());
    }

    /**
     * Get WIP limit status for a specific column.
     */
    @Transactional(readOnly = true)
    public WipLimitResponse getColumnWipLimit(UUID columnId) {
        BoardColumn column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSource.getMessage("error.column.not.found", new Object[]{columnId}, Locale.ENGLISH)));

        List<BoardIssueResponse> issues = issueServiceClient.fetchBoardIssues(column.getBoardId(), null);
        return createWipLimitResponse(column, issues);
    }

    /**
     * Update WIP limit for a column.
     */
    @Transactional
    public WipLimitResponse updateWipLimit(UUID boardId, UpdateWipLimitRequest request) {
        BoardColumn column = columnRepository.findById(request.getColumnId())
                .orElseThrow(() -> new ResourceNotFoundException(messageSource.getMessage("error.column.not.found", new Object[]{request.getColumnId()}, Locale.ENGLISH)));

        if (!column.getBoardId().equals(boardId)) {
            throw new ResourceNotFoundException(messageSource.getMessage("error.column.not.on.board", null, Locale.ENGLISH));
        }

        if (request.getWipLimit() != null) {
            column.setMaxIssues(request.getWipLimit());
        }

        if (request.getEnabled() != null && !request.getEnabled()) {
            column.setMaxIssues(null); // Disable limit
        }

        column = columnRepository.save(column);
        log.info("Updated WIP limit for column {} to {}", column.getId(), column.getMaxIssues());

        List<BoardIssueResponse> issues = issueServiceClient.fetchBoardIssues(boardId, null);
        return createWipLimitResponse(column, issues);
    }

    /**
     * Check if adding an issue to a column would exceed the WIP limit.
     */
    @Transactional(readOnly = true)
    public boolean canAddIssue(UUID columnId) {
        BoardColumn column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSource.getMessage("error.column.not.found", new Object[]{columnId}, Locale.ENGLISH)));

        if (column.getMaxIssues() == null || column.getMaxIssues() <= 0) {
            return true; // No limit set
        }

        List<BoardIssueResponse> issues = issueServiceClient.fetchBoardIssues(column.getBoardId(), null);
        int currentCount = countIssuesInColumn(column, issues);

        return currentCount < column.getMaxIssues();
    }

    /**
     * Validate move operation against WIP limits.
     * Returns true if the move is allowed, false otherwise.
     */
    @Transactional(readOnly = true)
    public boolean canMoveToColumn(UUID sourceColumnId, UUID targetColumnId) {
        if (sourceColumnId.equals(targetColumnId)) {
            return true; // Moving within same column, no WIP limit impact
        }

        BoardColumn targetColumn = columnRepository.findById(targetColumnId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSource.getMessage("error.column.not.found", new Object[]{targetColumnId}, Locale.ENGLISH)));

        if (targetColumn.getMaxIssues() == null || targetColumn.getMaxIssues() <= 0) {
            return true; // No limit set
        }

        List<BoardIssueResponse> issues = issueServiceClient.fetchBoardIssues(targetColumn.getBoardId(), null);
        int currentCount = countIssuesInColumn(targetColumn, issues);

        // Allow move if current count is below limit (the issue being moved hasn't been removed yet)
        return currentCount <= targetColumn.getMaxIssues();
    }

    /**
     * Get columns that have exceeded their WIP limits.
     */
    @Transactional(readOnly = true)
    public List<WipLimitResponse> getColumnsExceedingLimits(UUID boardId) {
        return getBoardWipLimits(boardId).stream()
                .filter(WipLimitResponse::getIsLimitExceeded)
                .collect(Collectors.toList());
    }

    /**
     * Get summary of WIP limits across the board.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWipLimitsSummary(UUID boardId) {
        List<WipLimitResponse> limits = getBoardWipLimits(boardId);

        long totalColumns = limits.size();
        long columnsWithLimits = limits.stream()
                .filter(l -> l.getWipLimit() != null && l.getWipLimit() > 0)
                .count();
        long columnsExceeding = limits.stream()
                .filter(WipLimitResponse::getIsLimitExceeded)
                .count();
        long totalIssues = limits.stream()
                .mapToInt(l -> l.getCurrentCount() != null ? l.getCurrentCount() : 0)
                .sum();
        long totalCapacity = limits.stream()
                .mapToInt(l -> l.getWipLimit() != null ? l.getWipLimit() : 0)
                .sum();

        Map<String, Object> summary = new HashMap<>();
        summary.put("boardId", boardId);
        summary.put("totalColumns", totalColumns);
        summary.put("columnsWithLimits", columnsWithLimits);
        summary.put("columnsExceedingLimits", columnsExceeding);
        summary.put("totalIssues", totalIssues);
        summary.put("totalCapacity", totalCapacity);
        summary.put("utilizationRate", totalCapacity > 0 ?
                Math.round((double) totalIssues / totalCapacity * 10000.0) / 100.0 : 0);
        summary.put("columns", limits);

        return summary;
    }

    /**
     * Create a WipLimitResponse from a column and current issues.
     */
    private WipLimitResponse createWipLimitResponse(BoardColumn column, List<BoardIssueResponse> issues) {
        int currentCount = countIssuesInColumn(column, issues);
        Integer wipLimit = column.getMaxIssues();
        boolean isEnabled = wipLimit != null && wipLimit > 0;

        return WipLimitResponse.builder()
                .boardId(column.getBoardId())
                .columnId(column.getId())
                .columnName(column.getName())
                .wipLimit(wipLimit)
                .currentCount(currentCount)
                .isLimitEnabled(isEnabled)
                .isLimitExceeded(isEnabled && currentCount > wipLimit)
                .build();
    }

    /**
     * Count issues in a column based on status mapping.
     */
    private int countIssuesInColumn(BoardColumn column, List<BoardIssueResponse> issues) {
        return (int) issues.stream()
                .filter(issue -> columnMatchesIssue(column, issue))
                .count();
    }

    /**
     * Check if an issue matches a column based on status.
     */
    private boolean columnMatchesIssue(BoardColumn column, BoardIssueResponse issue) {
        if (issue.getStatus() == null) {
            return false;
        }

        String issueStatus = normalize(issue.getStatus());
        String columnName = normalize(column.getName());

        // Direct status category matching
        String category = column.getStatusCategory();
        if (category != null) {
            if ("DONE".equals(category) || Boolean.TRUE.equals(column.getIsDone())) {
                return StatusCategoryHelper.isCompleted(issue.getStatus());
            }
            if ("IN_PROGRESS".equals(category)) {
                return StatusCategoryHelper.isInProgress(issue.getStatus());
            }
            if ("TODO".equals(category)) {
                return "TODO".equals(StatusCategoryHelper.getCategory(issue.getStatus()));
            }
        }

        // Name-based matching using StatusCategoryHelper
        String issueCategory = StatusCategoryHelper.getCategory(issue.getStatus());
        if (columnName.contains("done") || columnName.contains("complete")) {
            return "DONE".equals(issueCategory);
        }
        if (columnName.contains("progress") || columnName.contains("doing")) {
            return StatusCategoryHelper.isInProgress(issue.getStatus());
        }
        if (columnName.contains("review")) {
            return issueStatus.contains("review");
        }
        if (columnName.contains("backlog") || columnName.contains("todo")) {
            return "TODO".equals(issueCategory);
        }

        // Default: match by exact status name similarity
        return issueStatus.contains(columnName) || columnName.contains(issueStatus);
    }

    /**
     * Normalize string for comparison.
     */
    private String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase().replace("(legacy)", "").replace("(new)", "")
                .replaceAll("[\\s_\\-()]+", "").trim();
    }
}