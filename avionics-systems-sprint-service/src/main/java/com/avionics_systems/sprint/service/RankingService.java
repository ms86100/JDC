package com.avionics_systems.sprint.service;

import com.avionics_systems.board.dto.BoardIssueResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing issue ranking on agile boards using LexoRank algorithm.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RankingService {

    private final LexoRankService lexoRankService;
    private final IssueServiceClient issueServiceClient;

    /**
     * Move an issue to a specific position in a column.
     * Returns the new rank for the moved issue.
     */
    @Transactional
    public String moveIssue(UUID boardId, UUID issueId, String targetStatus, Integer targetIndex) {
        log.info("Moving issue {} to index {} in status {}", issueId, targetIndex, targetStatus);

        // Get current issues in the target column
        List<BoardIssueResponse> columnIssues = issueServiceClient.fetchBoardIssues(boardId, null)
                .stream()
                .filter(i -> statusMatches(i, targetStatus))
                .sorted(Comparator.comparing(i -> i.getRank() != null ? i.getRank() : lexoRankService.getMaxRank()))
                .collect(Collectors.toList());

        // Remove the moved issue if it exists in the list
        columnIssues.removeIf(i -> i.getId().equals(issueId));

        // If target index is beyond list size, add to end
        int safeIndex = Math.min(targetIndex, columnIssues.size());

        // Determine the ranks before and after insertion point
        String rankBefore = safeIndex > 0 ? columnIssues.get(safeIndex - 1).getRank() : null;
        String rankAfter = safeIndex < columnIssues.size() ? columnIssues.get(safeIndex).getRank() : null;

        // Generate new rank
        String newRank = lexoRankService.generateRankBetween(rankBefore, rankAfter);

        // Check if rebalancing is needed
        if (lexoRankService.needsRebalancing(rankBefore, newRank) ||
            lexoRankService.needsRebalancing(newRank, rankAfter)) {
            log.info("Rank rebalancing needed for board {}, column {}", boardId, targetStatus);
            rebalanceColumn(boardId, targetStatus);
            // Regenerate rank after rebalancing
            columnIssues = issueServiceClient.fetchBoardIssues(boardId, null)
                    .stream()
                    .filter(i -> statusMatches(i, targetStatus))
                    .sorted(Comparator.comparing(i -> i.getRank() != null ? i.getRank() : lexoRankService.getMaxRank()))
                    .collect(Collectors.toList());
            columnIssues.removeIf(i -> i.getId().equals(issueId));
            safeIndex = Math.min(targetIndex, columnIssues.size());
            rankBefore = safeIndex > 0 ? columnIssues.get(safeIndex - 1).getRank() : null;
            rankAfter = safeIndex < columnIssues.size() ? columnIssues.get(safeIndex).getRank() : null;
            newRank = lexoRankService.generateRankBetween(rankBefore, rankAfter);
        }

        // Update the issue's rank
        issueServiceClient.reorderIssueRank(issueId, newRank);

        return newRank;
    }

    /**
     * Move issue to the beginning of a column.
     */
    @Transactional
    public String moveIssueToTop(UUID boardId, UUID issueId, String targetStatus) {
        List<BoardIssueResponse> columnIssues = issueServiceClient.fetchBoardIssues(boardId, null)
                .stream()
                .filter(i -> statusMatches(i, targetStatus))
                .sorted(Comparator.comparing(i -> i.getRank() != null ? i.getRank() : lexoRankService.getMaxRank()))
                .collect(Collectors.toList());

        columnIssues.removeIf(i -> i.getId().equals(issueId));

        String firstRank = !columnIssues.isEmpty() ? columnIssues.get(0).getRank() : null;
        String newRank = lexoRankService.generateRankBefore(firstRank);

        issueServiceClient.reorderIssueRank(issueId, newRank);
        return newRank;
    }

    /**
     * Move issue to the end of a column.
     */
    @Transactional
    public String moveIssueToBottom(UUID boardId, UUID issueId, String targetStatus) {
        List<BoardIssueResponse> columnIssues = issueServiceClient.fetchBoardIssues(boardId, null)
                .stream()
                .filter(i -> statusMatches(i, targetStatus))
                .sorted(Comparator.comparing(i -> i.getRank() != null ? i.getRank() : lexoRankService.getMaxRank()))
                .collect(Collectors.toList());

        columnIssues.removeIf(i -> i.getId().equals(issueId));

        String lastRank = !columnIssues.isEmpty() ? columnIssues.get(columnIssues.size() - 1).getRank() : null;
        String newRank = lexoRankService.generateRankAfter(lastRank);

        issueServiceClient.reorderIssueRank(issueId, newRank);
        return newRank;
    }

    /**
     * Move issue after another issue in the same column.
     */
    @Transactional
    public String moveIssueAfter(UUID boardId, UUID issueId, UUID afterIssueId, String targetStatus) {
        List<BoardIssueResponse> columnIssues = issueServiceClient.fetchBoardIssues(boardId, null)
                .stream()
                .filter(i -> statusMatches(i, targetStatus))
                .sorted(Comparator.comparing(i -> i.getRank() != null ? i.getRank() : lexoRankService.getMaxRank()))
                .collect(Collectors.toList());

        // Find the position of the afterIssueId
        int afterIndex = -1;
        for (int i = 0; i < columnIssues.size(); i++) {
            if (columnIssues.get(i).getId().equals(afterIssueId)) {
                afterIndex = i;
                break;
            }
        }

        if (afterIndex < 0) {
            // Issue not found, move to end
            return moveIssueToBottom(boardId, issueId, targetStatus);
        }

        // Remove moved issue if present
        columnIssues.removeIf(i -> i.getId().equals(issueId));

        // Get ranks around the insertion point
        String rankBefore = columnIssues.get(afterIndex).getRank();
        String rankAfter = afterIndex + 1 < columnIssues.size() ? columnIssues.get(afterIndex + 1).getRank() : null;

        String newRank = lexoRankService.generateRankBetween(rankBefore, rankAfter);
        issueServiceClient.reorderIssueRank(issueId, newRank);

        return newRank;
    }

    /**
     * Rebalance ranks in a column after too many insertions.
     */
    @Transactional
    public void rebalanceColumn(UUID boardId, String status) {
        log.info("Rebalancing column {} on board {}", status, boardId);

        List<BoardIssueResponse> columnIssues = issueServiceClient.fetchBoardIssues(boardId, null)
                .stream()
                .filter(i -> statusMatches(i, status))
                .sorted(Comparator.comparing(i -> i.getRank() != null ? i.getRank() : lexoRankService.getMaxRank()))
                .collect(Collectors.toList());

        if (columnIssues.isEmpty()) {
            return;
        }

        // Generate new evenly spaced ranks
        List<String> newRanks = lexoRankService.generateRebalancedRanks(columnIssues.size());

        // Update each issue with its new rank
        for (int i = 0; i < columnIssues.size(); i++) {
            BoardIssueResponse issue = columnIssues.get(i);
            issueServiceClient.reorderIssueRank(issue.getId(), newRanks.get(i));
        }

        log.info("Rebalanced {} issues in column {} on board {}", columnIssues.size(), status, boardId);
    }

    /**
     * Get sorted issues for a column.
     */
    @Transactional(readOnly = true)
    public List<BoardIssueResponse> getSortedIssues(UUID boardId, String status) {
        return issueServiceClient.fetchBoardIssues(boardId, null)
                .stream()
                .filter(i -> statusMatches(i, status))
                .sorted(Comparator.comparing(i -> i.getRank() != null ? i.getRank() : lexoRankService.getMaxRank()))
                .collect(Collectors.toList());
    }

    /**
     * Initialize ranks for issues that don't have ranks.
     */
    @Transactional
    public void initializeRanks(UUID boardId) {
        List<BoardIssueResponse> issues = issueServiceClient.fetchBoardIssues(boardId, null);
        List<BoardIssueResponse> unranked = issues.stream()
                .filter(i -> i.getRank() == null || i.getRank().isBlank())
                .collect(Collectors.toList());

        if (unranked.isEmpty()) {
            return;
        }

        log.info("Initializing {} unranked issues on board {}", unranked.size(), boardId);

        String lastRank = issues.stream()
                .filter(i -> i.getRank() != null && !i.getRank().isBlank())
                .map(BoardIssueResponse::getRank)
                .max(lexoRankService::compare)
                .orElse(lexoRankService.getMinRank());

        for (BoardIssueResponse issue : unranked) {
            lastRank = lexoRankService.generateRankAfter(lastRank);
            issueServiceClient.reorderIssueRank(issue.getId(), lastRank);
        }
    }

    /**
     * Check if status matches the issue's status.
     */
    private boolean statusMatches(BoardIssueResponse issue, String targetStatus) {
        if (issue.getStatus() == null || targetStatus == null) {
            return false;
        }
        String normalizedTarget = targetStatus.toLowerCase().replaceAll("[\\s_\\-()]+", "");
        String normalizedIssue = issue.getStatus().toLowerCase().replaceAll("[\\s_\\-()]+", "");
        return normalizedIssue.contains(normalizedTarget) || normalizedTarget.contains(normalizedIssue);
    }
}