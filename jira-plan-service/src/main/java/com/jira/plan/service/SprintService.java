package com.jira.plan.service;

import com.jira.plan.dto.request.CreateSprintRequest;
import com.jira.plan.dto.response.SprintResponse;
import com.jira.plan.dto.response.SprintIssueResponse;
import com.jira.plan.dto.response.SprintBurndownResponse;
import com.jira.plan.entity.*;
import com.jira.plan.exception.ResourceNotFoundException;
import com.jira.plan.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sprint service with full lifecycle management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SprintService {

    private final SprintRepository sprintRepository;
    private final SprintIssueRepository sprintIssueRepository;
    private final SprintAuditLogRepository sprintAuditLogRepository;
    private final SprintBurndownRepository sprintBurndownRepository;
    private final BoardConfigRepository boardConfigRepository;
    private final PlanItemRepository planItemRepository;

    @Transactional(readOnly = true)
    public List<SprintResponse> getSprintsByBoardId(UUID boardId) {
        return sprintRepository.findByBoardConfigIdOrderBySequenceAsc(boardId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SprintResponse getSprintById(UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));
        return toResponse(sprint);
    }

    @Transactional
    public SprintResponse createSprint(UUID boardId, CreateSprintRequest request) {
        BoardConfig board = boardConfigRepository.findById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardConfig", "id", boardId));

        Integer nextSeq = sprintRepository.getMaxSequence(boardId);
        if (nextSeq == null) nextSeq = 0;
        else nextSeq = nextSeq + 1;

        Sprint sprint = Sprint.builder()
            .boardConfig(board)
            .name(request.getName())
            .goal(request.getGoal())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .state("FUTURE")
            .sequence(nextSeq)
            .build();

        sprint = sprintRepository.save(sprint);

        // Create audit log entry
        createAuditLog(sprint.getId(), "CREATED", null, null);

        return toResponse(sprint);
    }

    @Transactional
    public SprintResponse updateSprint(UUID sprintId, CreateSprintRequest request) {
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));

        if (request.getName() != null) sprint.setName(request.getName());
        if (request.getGoal() != null) sprint.setGoal(request.getGoal());
        if (request.getStartDate() != null) sprint.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) sprint.setEndDate(request.getEndDate());

        sprint = sprintRepository.save(sprint);

        createAuditLog(sprint.getId(), "UPDATED", null, null);

        return toResponse(sprint);
    }

    @Transactional
    public SprintResponse startSprint(UUID sprintId, UUID userId) {
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));

        if (!"FUTURE".equals(sprint.getState())) {
            throw new IllegalStateException("Can only start a FUTURE sprint");
        }

        // Close any currently active sprint on the same board
        boardConfigRepository.findById(sprint.getBoardConfig().getId()).ifPresent(board -> {
            sprintRepository.findByBoardConfigIdAndState(board.getId(), "ACTIVE")
                .ifPresent(activeSprint -> {
                    closeSprintInternal(activeSprint);
                });
        });

        sprint.start();
        sprint = sprintRepository.save(sprint);

        createAuditLog(sprint.getId(), "STARTED", userId, null);

        log.info("Sprint {} started at {}", sprint.getName(), sprint.getStartDate());

        return toResponse(sprint);
    }

    @Transactional
    public SprintResponse closeSprint(UUID sprintId, UUID userId) {
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));

        closeSprintInternal(sprint);
        createAuditLog(sprint.getId(), "CLOSED", userId, null);

        // Calculate velocity
        Integer completedPoints = sprintIssueRepository.sumCompletedPoints(sprintId);
        sprint.setCompletedPoints(completedPoints != null ? completedPoints : 0);

        Integer totalPoints = sprintIssueRepository.sumTotalPoints(sprintId);
        sprint.setCommittedPoints(totalPoints != null ? totalPoints : 0);
        sprint.setVelocity(completedPoints != null ? completedPoints : 0);

        sprint = sprintRepository.save(sprint);

        log.info("Sprint {} closed with velocity {}", sprint.getName(), sprint.getVelocity());

        return toResponse(sprint);
    }

    private void closeSprintInternal(Sprint sprint) {
        sprint.close();
        sprintRepository.save(sprint);

        // Mark remaining issues as incomplete
        List<SprintIssue> activeIssues = sprintIssueRepository.findActiveBySprintId(sprint.getId());
        for (SprintIssue issue : activeIssues) {
            if (!"COMPLETED".equals(issue.getCompletionStatus())) {
                issue.setCompletionStatus("INCOMPLETE");
                sprintIssueRepository.save(issue);
            }
        }
    }

    @Transactional
    public SprintResponse abandonSprint(UUID sprintId, UUID userId) {
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));

        sprint.abandon();
        sprint = sprintRepository.save(sprint);

        createAuditLog(sprint.getId(), "ABANDONED", userId, null);

        return toResponse(sprint);
    }

    @Transactional
    public SprintIssueResponse addIssueToSprint(UUID sprintId, UUID planItemId, UUID userId) {
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));

        if (!"ACTIVE".equals(sprint.getState())) {
            throw new IllegalStateException("Can only add issues to ACTIVE sprint");
        }

        PlanItem planItem = planItemRepository.findById(planItemId)
            .orElseThrow(() -> new ResourceNotFoundException("PlanItem", "id", planItemId));

        if (sprintIssueRepository.existsBySprintIdAndPlanItemId(sprintId, planItemId)) {
            throw new IllegalArgumentException("Issue already in sprint");
        }

        SprintIssue sprintIssue = SprintIssue.builder()
            .sprint(sprint)
            .planItem(planItem)
            .issueId(planItem.getIssueId())
            .rankValue(planItem.getSortOrder())
            .addedBy(userId)
            .completionStatus("UNCOMPLETED")
            .build();

        sprintIssue = sprintIssueRepository.save(sprintIssue);

        createAuditLog(sprint.getId(), "ISSUE_ADDED", userId,
            Map.of("planItemId", planItemId.toString(), "issueId", planItem.getIssueId().toString()));

        return toSprintIssueResponse(sprintIssue);
    }

    @Transactional
    public void removeIssueFromSprint(UUID sprintId, UUID planItemId, UUID userId) {
        SprintIssue sprintIssue = sprintIssueRepository.findBySprintIdAndPlanItemId(sprintId, planItemId)
            .orElseThrow(() -> new ResourceNotFoundException("SprintIssue not found"));

        sprintIssue.remove(userId);
        sprintIssueRepository.save(sprintIssue);

        createAuditLog(sprintId, "ISSUE_REMOVED", userId,
            Map.of("planItemId", planItemId.toString()));
    }

    @Transactional
    public SprintIssueResponse completeIssue(UUID sprintId, UUID planItemId) {
        SprintIssue sprintIssue = sprintIssueRepository.findBySprintIdAndPlanItemId(sprintId, planItemId)
            .orElseThrow(() -> new ResourceNotFoundException("SprintIssue not found"));

        sprintIssue.complete();
        sprintIssue = sprintIssueRepository.save(sprintIssue);

        return toSprintIssueResponse(sprintIssue);
    }

    @Transactional
    public SprintIssueResponse updateIssueColumn(UUID sprintId, UUID planItemId, String columnName) {
        SprintIssue sprintIssue = sprintIssueRepository.findBySprintIdAndPlanItemId(sprintId, planItemId)
            .orElseThrow(() -> new ResourceNotFoundException("SprintIssue not found"));

        // Update completion status based on column name
        String lowerColumnName = columnName.toLowerCase();
        if (lowerColumnName.contains("done") || lowerColumnName.contains("complete") || lowerColumnName.contains("closed")) {
            sprintIssue.complete();
        } else if (lowerColumnName.contains("progress")) {
            sprintIssue.setCompletionStatus("IN_PROGRESS");
        } else {
            sprintIssue.setCompletionStatus("UNCOMPLETED");
        }

        sprintIssue = sprintIssueRepository.save(sprintIssue);
        return toSprintIssueResponse(sprintIssue);
    }

    @Transactional(readOnly = true)
    public List<SprintIssueResponse> getSprintIssues(UUID sprintId) {
        return sprintIssueRepository.findBySprintId(sprintId).stream()
            .map(this::toSprintIssueResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SprintBurndownResponse getSprintBurndown(UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));

        List<SprintBurndown> snapshots = sprintBurndownRepository.findBySprintIdOrderBySnapshotDateAsc(sprintId);

        List<SprintBurndownResponse.BurndownPoint> points = snapshots.stream()
            .map(s -> SprintBurndownResponse.BurndownPoint.builder()
                .date(s.getSnapshotDate())
                .remainingIssues(s.getTotalIssues() - s.getCompletedIssues())
                .completedIssues(s.getCompletedIssues())
                .remainingPoints(s.getRemainingPoints())
                .idealRemaining(s.getIdealRemaining())
                .build())
            .collect(Collectors.toList());

        // Calculate summary stats
        int totalIssues = sprintIssueRepository.findActiveBySprintId(sprintId).size();
        int completedIssues = sprintIssueRepository.countCompletedBySprintId(sprintId);
        Integer totalPoints = sprintIssueRepository.sumTotalPoints(sprintId);
        Integer completedPoints = sprintIssueRepository.sumCompletedPoints(sprintId);

        return SprintBurndownResponse.builder()
            .sprintId(sprintId)
            .sprintName(sprint.getName())
            .startDate(sprint.getStartDate() != null ? sprint.getStartDate().toLocalDate() : null)
            .endDate(sprint.getEndDate() != null ? sprint.getEndDate().toLocalDate() : null)
            .totalIssues(totalIssues)
            .completedIssues(completedIssues)
            .totalPoints(totalPoints != null ? totalPoints : 0)
            .completedPoints(completedPoints != null ? completedPoints : 0)
            .burndownPoints(points)
            .build();
    }

    @Transactional
    public void takeBurndownSnapshot(UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));

        LocalDate today = LocalDate.now();

        if (sprintBurndownRepository.existsBySprintIdAndSnapshotDate(sprintId, today)) {
            log.debug("Burndown snapshot already exists for today");
            return;
        }

        List<SprintIssue> activeIssues = sprintIssueRepository.findActiveBySprintId(sprintId);
        int totalIssues = activeIssues.size();
        int completedIssues = (int) activeIssues.stream()
            .filter(i -> "COMPLETED".equals(i.getCompletionStatus()))
            .count();

        Integer totalPoints = sprintIssueRepository.sumTotalPoints(sprintId);
        Integer completedPoints = sprintIssueRepository.sumCompletedPoints(sprintId);

        // Calculate ideal remaining
        int daysTotal = 0;
        int daysRemaining = 0;
        if (sprint.getStartDate() != null && sprint.getEndDate() != null) {
            LocalDate start = sprint.getStartDate().toLocalDate();
            LocalDate end = sprint.getEndDate().toLocalDate();
            daysTotal = (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
            daysRemaining = (int) java.time.temporal.ChronoUnit.DAYS.between(today, end);
        }

        int idealRemaining = daysTotal > 0 ? (int) ((double) daysRemaining / daysTotal * totalIssues) : 0;

        SprintBurndown snapshot = SprintBurndown.builder()
            .sprintId(sprintId)
            .snapshotDate(today)
            .totalIssues(totalIssues)
            .completedIssues(completedIssues)
            .remainingPoints(totalPoints != null ? totalPoints - (completedPoints != null ? completedPoints : 0) : 0)
            .idealRemaining(idealRemaining)
            .build();

        sprintBurndownRepository.save(snapshot);
    }

    @Transactional(readOnly = true)
    public Double getAverageVelocity(UUID boardId) {
        return sprintRepository.getAverageVelocity(boardId);
    }

    private void createAuditLog(UUID sprintId, String eventType, UUID userId, Map<String, String> details) {
        String detailsJson = null;
        if (details != null) {
            try {
                detailsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(details);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                // Log and continue
            }
        }
        SprintAuditLog log = SprintAuditLog.builder()
            .sprintId(sprintId)
            .eventType(eventType)
            .userId(userId)
            .details(detailsJson)
            .build();
        sprintAuditLogRepository.save(log);
    }

    private SprintResponse toResponse(Sprint sprint) {
        List<SprintIssue> issues = sprintIssueRepository.findBySprintId(sprint.getId());
        int totalIssues = issues.size();
        int completedIssues = (int) issues.stream()
            .filter(i -> "COMPLETED".equals(i.getCompletionStatus()))
            .count();

        return SprintResponse.builder()
            .id(sprint.getId())
            .boardId(sprint.getBoardConfig() != null ? sprint.getBoardConfig().getId() : null)
            .name(sprint.getName())
            .goal(sprint.getGoal())
            .startDate(sprint.getStartDate())
            .endDate(sprint.getEndDate())
            .completeDate(sprint.getCompleteDate())
            .state(sprint.getState())
            .sequence(sprint.getSequence())
            .velocity(sprint.getVelocity())
            .committedPoints(sprint.getCommittedPoints())
            .completedPoints(sprint.getCompletedPoints())
            .totalIssues(totalIssues)
            .completedIssues(completedIssues)
            .createdAt(sprint.getCreatedAt())
            .updatedAt(sprint.getUpdatedAt())
            .build();
    }

    private SprintIssueResponse toSprintIssueResponse(SprintIssue issue) {
        return SprintIssueResponse.builder()
            .id(issue.getId())
            .sprintId(issue.getSprint().getId())
            .planItemId(issue.getPlanItem() != null ? issue.getPlanItem().getId() : null)
            .issueId(issue.getIssueId())
            .rankValue(issue.getRankValue())
            .addedAt(issue.getAddedAt())
            .addedBy(issue.getAddedBy())
            .removedAt(issue.getRemovedAt())
            .completionStatus(issue.getCompletionStatus())
            .completedAt(issue.getCompletedAt())
            .build();
    }
}