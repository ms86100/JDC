package com.jira.sprint.service;

import com.jira.board.dto.BoardIssueResponse;
import com.jira.sprint.dto.CreateSprintRequest;
import com.jira.sprint.dto.SprintResponse;
import com.jira.sprint.entity.Sprint;
import com.jira.sprint.entity.SprintIssue;
import com.jira.sprint.exception.ResourceNotFoundException;
import com.jira.sprint.repository.SprintIssueRepository;
import com.jira.sprint.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Sprint Planning Board operations.
 * Manages sprint planning, backlog issues, and sprint board state.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SprintPlanningService {

    private final SprintRepository sprintRepository;
    private final SprintIssueRepository sprintIssueRepository;
    private final IssueServiceClient issueServiceClient;
    private final LexoRankService lexoRankService;

    /**
     * Create a new sprint for a board.
     */
    @Transactional
    public SprintResponse createSprint(UUID boardId, UUID projectId, CreateSprintRequest request, UUID createdBy) {
        // Get max sequence for this board's sprints
        List<Sprint> existingSprints = sprintRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        int maxSequence = existingSprints.stream()
                .mapToInt(s -> s.getSequence() != null ? s.getSequence() : 0)
                .max()
                .orElse(0);

        Sprint sprint = Sprint.builder()
                .name(request.getName())
                .goal(request.getGoal())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Sprint.SprintStatus.PLANNING)
                .projectId(projectId)
                .boardId(boardId)
                .createdBy(createdBy)
                .sequence(maxSequence + 1)
                .build();

        sprint = sprintRepository.save(sprint);
        log.info("Created sprint {} for board {}", sprint.getId(), boardId);

        return SprintResponse.from(sprint);
    }

    /**
     * Get all sprints for a board with their states.
     */
    @Transactional(readOnly = true)
    public List<SprintBoardResponse> getBoardSprints(UUID boardId) {
        List<Sprint> sprints = sprintRepository.findByBoardIdOrderBySequenceAsc(boardId);

        return sprints.stream()
                .map(this::mapToSprintBoardResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get backlog issues that are not assigned to any active sprint.
     */
    @Transactional(readOnly = true)
    public BacklogResponse getBacklog(UUID boardId, UUID projectId, String jql) {
        // Get issues from issue service
        List<BoardIssueResponse> allIssues = issueServiceClient.fetchBoardIssues(projectId, jql);

        // Filter issues not assigned to any sprint
        List<BoardIssueResponse> backlogIssues = allIssues.stream()
                .filter(issue -> issue.getSprintId() == null)
                .sorted(Comparator.comparing(i -> i.getRank() != null ? i.getRank() : lexoRankService.getMaxRank()))
                .collect(Collectors.toList());

        // Calculate backlog metrics
        int totalPoints = backlogIssues.stream()
                .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0)
                .sum();

        Map<String, Integer> byType = backlogIssues.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getIssueType() != null ? i.getIssueType() : "Unknown",
                        Collectors.summingInt(i -> 1)));

        return BacklogResponse.builder()
                .boardId(boardId)
                .projectId(projectId)
                .issues(backlogIssues)
                .issueCount(backlogIssues.size())
                .totalPoints(totalPoints)
                .issuesByType(byType)
                .build();
    }

    /**
     * Get issues assigned to a specific sprint.
     */
    @Transactional(readOnly = true)
    public List<BoardIssueResponse> getSprintIssues(UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found: " + sprintId));

        List<UUID> issueIds = sprintIssueRepository.findBySprintIdOrderByOrderIndex(sprintId)
                .stream()
                .map(SprintIssue::getIssueId)
                .collect(Collectors.toList());

        if (issueIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Fetch issue details from issue service
        List<BoardIssueResponse> issues = issueServiceClient.fetchBoardIssues(sprint.getProjectId(), null);

        return issues.stream()
                .filter(issue -> issueIds.contains(issue.getId()))
                .sorted(Comparator.comparing(
                        issue -> {
                            int idx = issueIds.indexOf(issue.getId());
                            return idx >= 0 ? idx : Integer.MAX_VALUE;
                        }))
                .collect(Collectors.toList());
    }

    /**
     * Add issues from backlog to sprint during sprint planning.
     */
    @Transactional
    public SprintIssuesResponse addIssuesToSprint(UUID sprintId, List<UUID> issueIds, UUID addedBy) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found: " + sprintId));

        if (sprint.getStatus() == Sprint.SprintStatus.COMPLETED ||
            sprint.getStatus() == Sprint.SprintStatus.CLOSED) {
            throw new IllegalStateException("Cannot add issues to completed sprint");
        }

        int currentMaxOrder = sprintIssueRepository.findBySprintIdOrderByOrderIndex(sprintId)
                .stream()
                .mapToInt(SprintIssue::getOrderIndex)
                .max()
                .orElse(-1);

        List<SprintIssue> added = new ArrayList<>();
        for (UUID issueId : issueIds) {
            // Check if already in sprint
            if (sprintIssueRepository.findBySprintIdAndIssueId(sprintId, issueId).isEmpty()) {
                SprintIssue sprintIssue = SprintIssue.builder()
                        .sprintId(sprintId)
                        .issueId(issueId)
                        .orderIndex(++currentMaxOrder)
                        .build();
                sprintIssueRepository.save(sprintIssue);
                added.add(sprintIssue);
            }
        }

        log.info("Added {} issues to sprint {}", added.size(), sprintId);

        // Calculate sprint stats
        List<BoardIssueResponse> allSprintIssues = getSprintIssues(sprintId);
        int totalPoints = allSprintIssues.stream()
                .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0)
                .sum();
        int completedPoints = allSprintIssues.stream()
                .filter(i -> isCompletedStatus(i.getStatus()))
                .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0)
                .sum();

        return SprintIssuesResponse.builder()
                .sprintId(sprintId)
                .addedIssueCount(added.size())
                .totalIssueCount(allSprintIssues.size())
                .committedPoints(totalPoints)
                .completedPoints(completedPoints)
                .remainingPoints(totalPoints - completedPoints)
                .build();
    }

    /**
     * Remove issues from sprint (move back to backlog).
     */
    @Transactional
    public void removeIssuesFromSprint(UUID sprintId, List<UUID> issueIds) {
        for (UUID issueId : issueIds) {
            sprintIssueRepository.deleteBySprintIdAndIssueId(sprintId, issueId);
        }
        log.info("Removed {} issues from sprint {}", issueIds.size(), sprintId);
    }

    /**
     * Reorder issues within a sprint.
     */
    @Transactional
    public void reorderSprintIssues(UUID sprintId, List<UUID> issueIds) {
        for (int i = 0; i < issueIds.size(); i++) {
            final int index = i;
            UUID issueId = issueIds.get(i);
            sprintIssueRepository.findBySprintIdAndIssueId(sprintId, issueId)
                    .ifPresent(si -> {
                        si.setOrderIndex(index);
                        sprintIssueRepository.save(si);
                    });
        }
        log.info("Reordered {} issues in sprint {}", issueIds.size(), sprintId);
    }

    /**
     * Start a sprint (change status from PLANNING to ACTIVE).
     */
    @Transactional
    public SprintResponse startSprint(UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found: " + sprintId));

        if (sprint.getStatus() != Sprint.SprintStatus.PLANNING) {
            throw new IllegalStateException("Can only start sprints in PLANNING status");
        }

        // Complete any currently active sprints for this board
        if (sprint.getBoardId() != null) {
            List<Sprint> activeSprints = sprintRepository.findByBoardIdOrderBySequenceAsc(sprint.getBoardId())
                    .stream()
                    .filter(s -> s.getStatus() == Sprint.SprintStatus.ACTIVE)
                    .collect(Collectors.toList());

            for (Sprint active : activeSprints) {
                active.setStatus(Sprint.SprintStatus.CLOSED);
                active.setCompleteDate(LocalDate.now());
                sprintRepository.save(active);
            }
        }

        sprint.setStatus(Sprint.SprintStatus.ACTIVE);
        if (sprint.getStartDate() == null) {
            sprint.setStartDate(LocalDate.now());
        }

        sprint = sprintRepository.save(sprint);
        log.info("Started sprint {}", sprintId);

        return enrichSprintResponse(SprintResponse.from(sprint));
    }

    /**
     * Complete/close a sprint.
     */
    @Transactional
    public SprintResponse completeSprint(UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found: " + sprintId));

        sprint.setStatus(Sprint.SprintStatus.COMPLETED);
        if (sprint.getEndDate() == null) {
            sprint.setEndDate(LocalDate.now());
        }
        sprint.setCompleteDate(LocalDate.now());

        // Calculate velocity for this sprint
        List<BoardIssueResponse> issues = getSprintIssues(sprintId);
        int completedPoints = issues.stream()
                .filter(i -> isCompletedStatus(i.getStatus()))
                .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0)
                .sum();

        // Update sprint velocity tracking
        if (completedPoints > 0) {
            // Rolling average of last 3 sprints
            List<Sprint> recentSprints = sprintRepository.findByProjectIdOrderByCreatedAtDesc(sprint.getProjectId())
                    .stream()
                    .filter(s -> s.getVelocityPointAvg() != null && s.getStatus() == Sprint.SprintStatus.COMPLETED)
                    .limit(3)
                    .collect(Collectors.toList());

            double avg = recentSprints.stream()
                    .mapToDouble(s -> s.getVelocityPointAvg() != null ? s.getVelocityPointAvg() : 0)
                    .average()
                    .orElse(completedPoints);

            sprint.setVelocityPointAvg((avg + completedPoints) / 2);
        }

        sprint = sprintRepository.save(sprint);
        log.info("Completed sprint {}", sprintId);

        return enrichSprintResponse(SprintResponse.from(sprint));
    }

    /**
     * Get sprint planning data (sprint info + issues + backlog context).
     */
    @Transactional(readOnly = true)
    public SprintPlanningDataResponse getSprintPlanningData(UUID boardId, UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found: " + sprintId));

        List<BoardIssueResponse> sprintIssues = getSprintIssues(sprintId);
        BacklogResponse backlog = getBacklog(boardId, sprint.getProjectId(), null);

        // Calculate sprint capacity vs commitment
        int committedPoints = sprintIssues.stream()
                .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0)
                .sum();

        Integer capacity = sprint.getCapacity();
        if (capacity == null) {
            capacity = committedPoints; // Use commitment as capacity
        }

        return SprintPlanningDataResponse.builder()
                .sprint(SprintResponse.from(sprint))
                .sprintIssues(sprintIssues)
                .backlog(backlog)
                .committedPoints(committedPoints)
                .capacity(capacity)
                .capacityRemaining(Math.max(0, capacity - committedPoints))
                .isOverCommitted(committedPoints > capacity)
                .build();
    }

    private SprintBoardResponse mapToSprintBoardResponse(Sprint sprint) {
        List<BoardIssueResponse> issues = getSprintIssues(sprint.getId());

        int totalPoints = issues.stream()
                .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0)
                .sum();
        int completedPoints = issues.stream()
                .filter(i -> isCompletedStatus(i.getStatus()))
                .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0)
                .sum();

        return SprintBoardResponse.builder()
                .id(sprint.getId())
                .name(sprint.getName())
                .goal(sprint.getGoal())
                .status(sprint.getStatus().name())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .completeDate(sprint.getCompleteDate())
                .issueCount(issues.size())
                .completedIssueCount((int) issues.stream().filter(i -> isCompletedStatus(i.getStatus())).count())
                .committedPoints(totalPoints)
                .completedPoints(completedPoints)
                .remainingPoints(totalPoints - completedPoints)
                .build();
    }

    private SprintResponse enrichSprintResponse(SprintResponse response) {
        try {
            int issueCount = sprintIssueRepository.countBySprintId(response.getId());
            response.setIssueCount(issueCount);
            response.setCompletedIssueCount(0); // Would need status check from issue service
            return response;
        } catch (Exception e) {
            log.warn("Error counting issues for sprint {}: {}", response.getId(), e.getMessage());
            response.setIssueCount(0);
            response.setCompletedIssueCount(0);
            return response;
        }
    }

    private boolean isCompletedStatus(String status) {
        if (status == null) return false;
        String normalized = status.toLowerCase();
        return normalized.contains("done") ||
               normalized.contains("completed") ||
               normalized.contains("closed") ||
               normalized.equals("resolved");
    }

    // DTO classes
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SprintBoardResponse {
        private UUID id;
        private String name;
        private String goal;
        private String status;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate completeDate;
        private Integer issueCount;
        private Integer completedIssueCount;
        private Integer committedPoints;
        private Integer completedPoints;
        private Integer remainingPoints;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BacklogResponse {
        private UUID boardId;
        private UUID projectId;
        private List<BoardIssueResponse> issues;
        private Integer issueCount;
        private Integer totalPoints;
        private Map<String, Integer> issuesByType;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SprintIssuesResponse {
        private UUID sprintId;
        private Integer addedIssueCount;
        private Integer totalIssueCount;
        private Integer committedPoints;
        private Integer completedPoints;
        private Integer remainingPoints;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SprintPlanningDataResponse {
        private SprintResponse sprint;
        private List<BoardIssueResponse> sprintIssues;
        private BacklogResponse backlog;
        private Integer committedPoints;
        private Integer capacity;
        private Integer capacityRemaining;
        private Boolean isOverCommitted;
    }
}