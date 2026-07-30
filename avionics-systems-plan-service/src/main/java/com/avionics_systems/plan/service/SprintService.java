package com.avionics_systems.plan.service;

import com.avionics_systems.cluster.util.StatusCategoryHelper;
import com.avionics_systems.plan.dto.request.*;
import com.avionics_systems.plan.dto.response.*;
import com.avionics_systems.plan.entity.*;
import com.avionics_systems.plan.exception.ResourceNotFoundException;
import com.avionics_systems.plan.repository.*;
import com.avionics_systems.plan.specification.SprintIssueSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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
    private final WorkingDaysService workingDaysService;
    private final SprintSnapshotService sprintSnapshotService;
    private final SprintPropertyRepository sprintPropertyRepository;
    private final SprintEventRepository sprintEventRepository;
    private final CumulativeFlowSnapshotRepository cumulativeFlowSnapshotRepository;

    @Value("${app.sprint.state.future:FUTURE}")
    private String sprintStateFuture;

    @Value("${app.sprint.state.active:ACTIVE}")
    private String sprintStateActive;

    @Value("${app.sprint.state.closed:CLOSED}")
    private String sprintStateClosed;

    @Value("${app.sprint.state.abandoned:ABANDONED}")
    private String sprintStateAbandoned;

    @Value("${app.sprint.completion-status.uncompleted:UNCOMPLETED}")
    private String completionStatusUncompleted;

    @Value("${app.sprint.completion-status.completed:COMPLETED}")
    private String completionStatusCompleted;

    @Value("${app.sprint.completion-status.incomplete:INCOMPLETE}")
    private String completionStatusIncomplete;

    @Value("${app.sprint.completion-status.dropped:DROPPED}")
    private String completionStatusDropped;

    @Value("${app.sprint.completion-status.in-progress:IN_PROGRESS}")
    private String completionStatusInProgress;

    @Value("${app.sprint.default-rank:0|hzzzzz:}")
    private String defaultRankValue;

    // ==================== SPRINT CRUD ====================

    @Transactional(readOnly = true)
    public List<SprintResponse> getSprintsByBoardId(UUID boardId) {
        List<Sprint> sprints = sprintRepository.findByBoardConfigIdOrderBySequenceAsc(boardId);
        if (sprints.isEmpty()) return List.of();

        List<UUID> sprintIds = sprints.stream().map(Sprint::getId).collect(Collectors.toList());
        List<SprintIssue> allIssues = sprintIssueRepository.findBySprintIds(sprintIds);
        Map<UUID, List<SprintIssue>> issuesBySprint = allIssues.stream()
                .collect(Collectors.groupingBy(si -> si.getSprint().getId()));

        return sprints.stream()
                .map(sprint -> toResponseWithIssues(sprint, issuesBySprint.getOrDefault(sprint.getId(), List.of())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<SprintResponse> getSprintsByBoardIdPaginated(UUID boardId, String state, int startAt, int maxResults) {
        Pageable pageable = PageRequest.of(startAt / Math.max(maxResults, 1), Math.max(maxResults, 1));
        Page<Sprint> page;
        if (state != null && !state.isBlank()) {
            if (state.contains(",")) {
                List<String> states = Arrays.stream(state.split(",")).map(String::trim).collect(Collectors.toList());
                page = sprintRepository.findByBoardConfigIdAndStateInPaginated(boardId, states, pageable);
            } else {
                page = sprintRepository.findByBoardConfigIdAndStatePaginated(boardId, state.toUpperCase(), pageable);
            }
        } else {
            page = sprintRepository.findByBoardConfigIdPaginated(boardId, pageable);
        }

        List<UUID> sprintIds = page.getContent().stream().map(Sprint::getId).collect(Collectors.toList());
        List<SprintIssue> allIssues = sprintIds.isEmpty() ? List.of() : sprintIssueRepository.findBySprintIds(sprintIds);
        Map<UUID, List<SprintIssue>> issuesBySprint = allIssues.stream()
                .collect(Collectors.groupingBy(si -> si.getSprint().getId()));

        List<SprintResponse> values = page.getContent().stream()
                .map(s -> toResponseWithIssues(s, issuesBySprint.getOrDefault(s.getId(), List.of())))
                .collect(Collectors.toList());

        return PaginatedResponse.<SprintResponse>builder()
                .startAt(startAt).maxResults(maxResults)
                .total((int) page.getTotalElements()).isLast(page.isLast())
                .values(values).build();
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

        Integer nextSeq = sprintRepository.getMaxSequenceWithLock(boardId).orElse(0) + 1;

        Sprint sprint = Sprint.builder()
            .boardConfig(board).name(request.getName()).goal(request.getGoal())
            .startDate(request.getStartDate()).endDate(request.getEndDate())
            .state(sprintStateFuture).sequence(nextSeq).wipLimit(request.getWipLimit()).build();

        sprint = sprintRepository.save(sprint);
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

    // Gap 1: Partial update
    @Transactional
    public SprintResponse partialUpdateSprint(UUID sprintId, PartialUpdateSprintRequest request) {
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));

        if (request.getName() != null) sprint.setName(request.getName());
        if (request.getGoal() != null) sprint.setGoal(request.getGoal());
        if (request.getStartDate() != null) sprint.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) sprint.setEndDate(request.getEndDate());
        if (request.getWipLimit() != null) sprint.setWipLimit(request.getWipLimit());

        sprint = sprintRepository.save(sprint);
        createAuditLog(sprint.getId(), "UPDATED", null, null);
        return toResponse(sprint);
    }

    @Transactional
    public void deleteSprint(UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));

        if (sprintStateActive.equals(sprint.getState())) {
            throw new IllegalStateException("Cannot delete an active sprint. Close or abandon it first.");
        }

        List<SprintIssue> activeIssues = sprintIssueRepository.findActiveBySprintId(sprintId);
        for (SprintIssue issue : activeIssues) {
            issue.remove(null);
            sprintIssueRepository.save(issue);
        }

        createAuditLog(sprintId, "DELETED", null, null);
        sprintRepository.delete(sprint);
    }

    // ==================== SPRINT LIFECYCLE ====================

    @Transactional
    public SprintResponse startSprint(UUID sprintId, UUID userId) {
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));

        if (!sprintStateFuture.equals(sprint.getState())) {
            throw new IllegalStateException("Can only start a FUTURE sprint");
        }

        // Gap 21: Only auto-close if parallel sprints NOT enabled
        BoardConfig board = sprint.getBoardConfig();
        if (!Boolean.TRUE.equals(board.getFeatureParallelSprints())) {
            sprintRepository.findByBoardConfigIdAndState(board.getId(), sprintStateActive)
                .ifPresent(this::closeSprintInternal);
        }

        sprint.start();
        sprint = sprintRepository.save(sprint);
        createAuditLog(sprint.getId(), "STARTED", userId, null);

        return toResponse(sprint);
    }

    @Transactional
    public SprintResponse closeSprint(UUID sprintId, UUID userId) {
        return closeSprint(sprintId, userId, null);
    }

    // Gap 20: Close sprint with optional incomplete issue move
    @Transactional
    public SprintResponse closeSprint(UUID sprintId, UUID userId, CloseSprintRequest request) {
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));

        closeSprintInternal(sprint);
        createAuditLog(sprint.getId(), "CLOSED", userId, null);

        Integer completedPoints = sprintIssueRepository.sumCompletedPoints(sprintId);
        sprint.setCompletedPoints(completedPoints != null ? completedPoints : 0);
        Integer totalPoints = sprintIssueRepository.sumTotalPoints(sprintId);
        sprint.setCommittedPoints(totalPoints != null ? totalPoints : 0);
        sprint.setVelocity(completedPoints != null ? completedPoints : 0);
        sprint = sprintRepository.save(sprint);

        // Move incomplete issues to target sprint if specified
        if (request != null && request.getMoveIncompleteToSprintId() != null) {
            UUID targetSprintId = request.getMoveIncompleteToSprintId();
            Sprint targetSprint = sprintRepository.findById(targetSprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", targetSprintId));

            List<SprintIssue> incompleteIssues = sprintIssueRepository.findActiveBySprintId(sprintId).stream()
                .filter(i -> !completionStatusCompleted.equals(i.getCompletionStatus()))
                .collect(Collectors.toList());

            for (SprintIssue issue : incompleteIssues) {
                issue.remove(userId);
                sprintIssueRepository.save(issue);

                SprintIssue newLink = SprintIssue.builder()
                    .sprint(targetSprint).planItem(issue.getPlanItem())
                    .issueId(issue.getIssueId()).rankValue(issue.getRankValue())
                    .addedBy(userId).completionStatus(completionStatusUncompleted).build();
                sprintIssueRepository.save(newLink);
            }

            createAuditLog(targetSprintId, "ISSUES_MOVED_FROM_CLOSED_SPRINT", userId,
                Map.of("sourceSprintId", sprintId.toString(), "count", String.valueOf(incompleteIssues.size())));
        }

        return toResponse(sprint);
    }

    private void closeSprintInternal(Sprint sprint) {
        sprint.close();
        sprintRepository.save(sprint);

        List<SprintIssue> activeIssues = sprintIssueRepository.findActiveBySprintId(sprint.getId());
        for (SprintIssue issue : activeIssues) {
            if (!completionStatusCompleted.equals(issue.getCompletionStatus())) {
                issue.setCompletionStatus(completionStatusIncomplete);
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

    // Gap 16: Reopen sprint
    @Transactional
    public SprintResponse reopenSprint(UUID sprintId, UUID userId) {
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));
        sprint.reopen();
        sprint = sprintRepository.save(sprint);
        createAuditLog(sprint.getId(), "REOPENED", userId, null);
        return toResponse(sprint);
    }

    // Gap 2: Swap sprint order
    @Transactional
    public void swapSprintOrder(UUID sprintId, SwapSprintRequest request) {
        Sprint a = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));
        Sprint b = sprintRepository.findById(request.getSprintToSwapWith())
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", request.getSprintToSwapWith()));

        if (!a.isFuture() || !b.isFuture()) {
            throw new IllegalStateException("Both sprints must be in FUTURE state to swap");
        }
        if (!a.getBoardConfig().getId().equals(b.getBoardConfig().getId())) {
            throw new IllegalArgumentException("Both sprints must belong to the same board");
        }

        Integer tempSeq = a.getSequence();
        a.setSequence(b.getSequence());
        b.setSequence(tempSeq);
        sprintRepository.save(a);
        sprintRepository.save(b);
        createAuditLog(sprintId, "REORDERED", null, Map.of("swappedWith", request.getSprintToSwapWith().toString()));
    }

    // ==================== SPRINT ISSUES ====================

    @Transactional(readOnly = true)
    public List<SprintIssueResponse> getSprintIssues(UUID sprintId) {
        return sprintIssueRepository.findBySprintId(sprintId).stream()
            .map(this::toSprintIssueResponse).collect(Collectors.toList());
    }

    // Gap 7: Board-scoped sprint issues
    @Transactional(readOnly = true)
    public List<SprintIssueResponse> getBoardSprintIssues(UUID boardId, UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));
        if (!sprint.getBoardConfig().getId().equals(boardId)) {
            throw new IllegalArgumentException("Sprint does not belong to this board");
        }
        return sprintIssueRepository.findActiveBySprintId(sprintId).stream()
            .map(this::toSprintIssueResponse).collect(Collectors.toList());
    }

    // Gap 9: JQL filtering and pagination
    @Transactional(readOnly = true)
    public PaginatedResponse<SprintIssueResponse> getSprintIssuesFiltered(UUID sprintId, String jql, int startAt, int maxResults) {
        Pageable pageable = PageRequest.of(startAt / Math.max(maxResults, 1), Math.max(maxResults, 1));
        Page<SprintIssue> page = sprintIssueRepository.findAll(
            SprintIssueSpecification.buildFromJql(sprintId, jql), pageable);

        List<SprintIssueResponse> values = page.getContent().stream()
            .map(this::toSprintIssueResponse).collect(Collectors.toList());

        return PaginatedResponse.<SprintIssueResponse>builder()
            .startAt(startAt).maxResults(maxResults)
            .total((int) page.getTotalElements()).isLast(page.isLast())
            .values(values).build();
    }

    @Transactional
    public SprintIssueResponse addIssueToSprint(UUID sprintId, UUID planItemId, UUID userId) {
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));

        if (!sprintStateActive.equals(sprint.getState()) && !sprintStateFuture.equals(sprint.getState())) {
            throw new IllegalStateException("Can only add issues to ACTIVE or FUTURE sprints");
        }

        if (sprint.getWipLimit() != null && sprint.getWipLimit() > 0) {
            int currentIssueCount = sprintIssueRepository.findActiveBySprintId(sprintId).size();
            if (currentIssueCount >= sprint.getWipLimit()) {
                throw new IllegalStateException("Sprint WIP limit (" + sprint.getWipLimit() + ") reached.");
            }
        }

        PlanItem planItem = planItemRepository.findById(planItemId)
            .orElseThrow(() -> new ResourceNotFoundException("PlanItem", "id", planItemId));

        if (sprintIssueRepository.existsBySprintIdAndPlanItemId(sprintId, planItemId)) {
            throw new IllegalArgumentException("Issue already in sprint");
        }

        SprintIssue sprintIssue = SprintIssue.builder()
            .sprint(sprint).planItem(planItem).issueId(planItem.getIssueId())
            .rankValue(planItem.getSortOrder()).addedBy(userId)
            .completionStatus(completionStatusUncompleted).build();

        sprintIssue = sprintIssueRepository.save(sprintIssue);

        // Gap 11: Record sprint event
        recordSprintEvent(sprintId, "ISSUE_ADDED", planItemId,
            null, planItem.getStoryPoints(),
            planItem.getStoryPoints() != null ? planItem.getStoryPoints() : 0, userId);

        createAuditLog(sprint.getId(), "ISSUE_ADDED", userId,
            Map.of("planItemId", planItemId.toString(), "issueId", planItem.getIssueId().toString()));

        return toSprintIssueResponse(sprintIssue);
    }

    @Transactional
    public void removeIssueFromSprint(UUID sprintId, UUID planItemId, UUID userId) {
        SprintIssue sprintIssue = sprintIssueRepository.findBySprintIdAndPlanItemId(sprintId, planItemId)
            .orElseThrow(() -> new ResourceNotFoundException("SprintIssue not found"));

        Integer points = sprintIssue.getPlanItem() != null ? sprintIssue.getPlanItem().getStoryPoints() : null;
        sprintIssue.remove(userId);
        sprintIssueRepository.save(sprintIssue);

        recordSprintEvent(sprintId, "ISSUE_REMOVED", planItemId,
            points, null, points != null ? -points : 0, userId);

        createAuditLog(sprintId, "ISSUE_REMOVED", userId, Map.of("planItemId", planItemId.toString()));
    }

    @Transactional
    public SprintIssueResponse completeIssue(UUID sprintId, UUID planItemId) {
        SprintIssue sprintIssue = sprintIssueRepository.findBySprintIdAndPlanItemId(sprintId, planItemId)
            .orElseThrow(() -> new ResourceNotFoundException("SprintIssue not found"));
        sprintIssue.complete();
        sprintIssue = sprintIssueRepository.save(sprintIssue);

        Integer points = sprintIssue.getPlanItem() != null ? sprintIssue.getPlanItem().getStoryPoints() : null;
        recordSprintEvent(sprintId, "ISSUE_COMPLETED", planItemId, null, points, 0, null);

        return toSprintIssueResponse(sprintIssue);
    }

    @Transactional
    public SprintIssueResponse updateIssueColumn(UUID sprintId, UUID planItemId, String columnName) {
        SprintIssue sprintIssue = sprintIssueRepository.findBySprintIdAndPlanItemId(sprintId, planItemId)
            .orElseThrow(() -> new ResourceNotFoundException("SprintIssue not found"));

        if (StatusCategoryHelper.isCompleted(columnName)) {
            sprintIssue.complete();
        } else if (StatusCategoryHelper.isInProgress(columnName)) {
            sprintIssue.setCompletionStatus(completionStatusInProgress);
        } else {
            sprintIssue.setCompletionStatus(completionStatusUncompleted);
        }

        sprintIssue = sprintIssueRepository.save(sprintIssue);
        return toSprintIssueResponse(sprintIssue);
    }

    // Gap 4: Bulk move issues to sprint
    @Transactional
    public BulkMoveIssuesResponse bulkMoveIssuesToSprint(UUID targetSprintId, BulkMoveIssuesRequest request, UUID userId) {
        Sprint targetSprint = sprintRepository.findById(targetSprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", targetSprintId));

        List<SprintIssueResponse> results = new ArrayList<>();
        int removedCount = 0;

        for (UUID planItemId : request.getIssueIds()) {
            List<SprintIssue> existingLinks = sprintIssueRepository.findByPlanItemIdAndRemovedAtIsNull(planItemId);
            for (SprintIssue existing : existingLinks) {
                if (!existing.getSprint().getId().equals(targetSprintId)) {
                    existing.remove(userId);
                    sprintIssueRepository.save(existing);
                    removedCount++;
                }
            }

            if (sprintIssueRepository.existsBySprintIdAndPlanItemId(targetSprintId, planItemId)) continue;

            PlanItem planItem = planItemRepository.findById(planItemId)
                .orElseThrow(() -> new ResourceNotFoundException("PlanItem", "id", planItemId));

            SprintIssue sprintIssue = SprintIssue.builder()
                .sprint(targetSprint).planItem(planItem).issueId(planItem.getIssueId())
                .rankValue(planItem.getSortOrder()).addedBy(userId)
                .completionStatus(completionStatusUncompleted).build();
            sprintIssue = sprintIssueRepository.save(sprintIssue);
            results.add(toSprintIssueResponse(sprintIssue));
        }

        createAuditLog(targetSprintId, "BULK_ISSUES_ADDED", userId,
            Map.of("count", String.valueOf(request.getIssueIds().size())));

        return BulkMoveIssuesResponse.builder()
            .addedCount(results.size()).removedFromPreviousCount(removedCount)
            .movedIssues(results).build();
    }

    // Gap 5: Move issues to backlog
    @Transactional
    public void moveIssuesToBacklog(MoveToBacklogRequest request, UUID userId) {
        for (UUID planItemId : request.getPlanItemIds()) {
            List<SprintIssue> activeLinks = sprintIssueRepository.findByPlanItemIdAndRemovedAtIsNull(planItemId);
            for (SprintIssue link : activeLinks) {
                link.remove(userId);
                sprintIssueRepository.save(link);
                createAuditLog(link.getSprint().getId(), "ISSUE_MOVED_TO_BACKLOG", userId,
                    Map.of("planItemId", planItemId.toString()));
            }
        }
    }

    // Gap 6: Rank issue
    @Transactional
    public SprintIssueResponse rankIssue(UUID planItemId, RankIssueRequest request) {
        SprintIssue issue = sprintIssueRepository.findBySprintIdAndPlanItemId(request.getSprintId(), planItemId)
            .orElseThrow(() -> new ResourceNotFoundException("SprintIssue not found"));

        String rankBefore = null, rankAfter = null;
        if (request.getRankBeforeIssue() != null) {
            rankBefore = sprintIssueRepository.findBySprintIdAndPlanItemId(request.getSprintId(), request.getRankBeforeIssue())
                .map(SprintIssue::getRankValue).orElse(null);
        }
        if (request.getRankAfterIssue() != null) {
            rankAfter = sprintIssueRepository.findBySprintIdAndPlanItemId(request.getSprintId(), request.getRankAfterIssue())
                .map(SprintIssue::getRankValue).orElse(null);
        }

        String newRank = calculateRankBetween(rankAfter, rankBefore);
        issue.setRankValue(newRank);
        issue = sprintIssueRepository.save(issue);
        return toSprintIssueResponse(issue);
    }

    // Gap 19: Toggle flag
    @Transactional
    public SprintIssueResponse toggleFlag(UUID sprintId, UUID planItemId, boolean flagged, String reason) {
        SprintIssue si = sprintIssueRepository.findBySprintIdAndPlanItemId(sprintId, planItemId)
            .orElseThrow(() -> new ResourceNotFoundException("SprintIssue not found"));
        si.setFlagged(flagged);
        si.setFlagReason(flagged ? reason : null);
        si = sprintIssueRepository.save(si);
        return toSprintIssueResponse(si);
    }

    // Gap 19: Get closed sprints for an issue
    @Transactional(readOnly = true)
    public List<SprintResponse> getClosedSprintsForIssue(UUID planItemId) {
        return sprintIssueRepository.findByPlanItemIdAndRemovedAtIsNull(planItemId).stream()
            .map(si -> si.getSprint())
            .filter(s -> sprintStateClosed.equals(s.getState()))
            .distinct()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    // Gap 17: Get issue estimation
    @Transactional(readOnly = true)
    public IssueEstimationResponse getIssueEstimation(UUID boardId, UUID planItemId) {
        PlanItem item = planItemRepository.findById(planItemId)
            .orElseThrow(() -> new ResourceNotFoundException("PlanItem", "id", planItemId));
        return IssueEstimationResponse.builder()
            .planItemId(planItemId).boardId(boardId).storyPoints(item.getStoryPoints()).build();
    }

    // Gap 17: Update issue estimation
    @Transactional
    public IssueEstimationResponse updateIssueEstimation(UUID boardId, UUID planItemId, Integer storyPoints, UUID userId) {
        PlanItem item = planItemRepository.findById(planItemId)
            .orElseThrow(() -> new ResourceNotFoundException("PlanItem", "id", planItemId));
        Integer oldPoints = item.getStoryPoints();
        item.setStoryPoints(storyPoints);
        planItemRepository.save(item);

        sprintIssueRepository.findByPlanItemIdAndRemovedAtIsNull(planItemId).forEach(si -> {
            if (si.getSprint().isActive()) {
                recordSprintEvent(si.getSprint().getId(), "ESTIMATE_CHANGED", planItemId,
                    oldPoints, storyPoints,
                    (storyPoints != null ? storyPoints : 0) - (oldPoints != null ? oldPoints : 0), userId);
            }
        });

        return IssueEstimationResponse.builder()
            .planItemId(planItemId).boardId(boardId).storyPoints(storyPoints).build();
    }

    // ==================== SPRINT PROPERTIES (Gap 3) ====================

    @Transactional(readOnly = true)
    public List<SprintPropertyResponse> getSprintProperties(UUID sprintId) {
        return sprintPropertyRepository.findBySprintId(sprintId).stream()
            .map(p -> SprintPropertyResponse.builder().key(p.getPropertyKey()).value(p.getPropertyValue()).build())
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SprintPropertyResponse getSprintProperty(UUID sprintId, String key) {
        SprintProperty prop = sprintPropertyRepository.findBySprintIdAndPropertyKey(sprintId, key)
            .orElseThrow(() -> new ResourceNotFoundException("SprintProperty", "key", key));
        return SprintPropertyResponse.builder().key(prop.getPropertyKey()).value(prop.getPropertyValue()).build();
    }

    @Transactional
    public SprintPropertyResponse setSprintProperty(UUID sprintId, String key, String value) {
        SprintProperty prop = sprintPropertyRepository.findBySprintIdAndPropertyKey(sprintId, key)
            .orElse(SprintProperty.builder().sprintId(sprintId).propertyKey(key).build());
        prop.setPropertyValue(value);
        sprintPropertyRepository.save(prop);
        return SprintPropertyResponse.builder().key(key).value(value).build();
    }

    @Transactional
    public void deleteSprintProperty(UUID sprintId, String key) {
        sprintPropertyRepository.deleteBySprintIdAndPropertyKey(sprintId, key);
    }

    // ==================== BURNDOWN & VELOCITY ====================

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
                .idealRemaining(s.getIdealRemaining()).build())
            .collect(Collectors.toList());

        int totalIssues = sprintIssueRepository.findActiveBySprintId(sprintId).size();
        int completedIssues = sprintIssueRepository.countCompletedBySprintId(sprintId);
        Integer totalPoints = sprintIssueRepository.sumTotalPoints(sprintId);
        Integer completedPoints = sprintIssueRepository.sumCompletedPoints(sprintId);

        return SprintBurndownResponse.builder()
            .sprintId(sprintId).sprintName(sprint.getName())
            .startDate(sprint.getStartDate() != null ? sprint.getStartDate().toLocalDate() : null)
            .endDate(sprint.getEndDate() != null ? sprint.getEndDate().toLocalDate() : null)
            .totalIssues(totalIssues).completedIssues(completedIssues)
            .totalPoints(totalPoints != null ? totalPoints : 0)
            .completedPoints(completedPoints != null ? completedPoints : 0)
            .burndownPoints(points).build();
    }

    // Gap 11: Event-based burndown
    @Transactional(readOnly = true)
    public EventBurndownResponse getEventBasedBurndown(UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));
        List<SprintEvent> events = sprintEventRepository.findBySprintIdOrderByEventTimestampAsc(sprintId);
        List<SprintBurndown> dailySnapshots = sprintBurndownRepository.findBySprintIdOrderBySnapshotDateAsc(sprintId);

        List<EventBurndownResponse.BurndownEvent> burndownEvents = events.stream()
            .map(e -> EventBurndownResponse.BurndownEvent.builder()
                .timestamp(e.getEventTimestamp()).eventType(e.getEventType())
                .planItemId(e.getPlanItemId()).pointsDelta(e.getPointsDelta())
                .oldValue(e.getOldValue()).newValue(e.getNewValue()).build())
            .collect(Collectors.toList());

        List<SprintBurndownResponse.BurndownPoint> snapshots = dailySnapshots.stream()
            .map(s -> SprintBurndownResponse.BurndownPoint.builder()
                .date(s.getSnapshotDate())
                .remainingIssues(s.getTotalIssues() - s.getCompletedIssues())
                .completedIssues(s.getCompletedIssues())
                .remainingPoints(s.getRemainingPoints())
                .idealRemaining(s.getIdealRemaining()).build())
            .collect(Collectors.toList());

        return EventBurndownResponse.builder()
            .sprintId(sprintId).startTime(sprint.getStartDate()).endTime(sprint.getEndDate())
            .events(burndownEvents).dailySnapshots(snapshots).build();
    }

    @Transactional
    public void takeBurndownSnapshot(UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));

        LocalDate today = LocalDate.now();
        if (sprintBurndownRepository.existsBySprintIdAndSnapshotDate(sprintId, today)) return;

        List<SprintIssue> activeIssues = sprintIssueRepository.findActiveBySprintId(sprintId);
        int totalIssues = activeIssues.size();
        int completedIssues = (int) activeIssues.stream()
            .filter(i -> completionStatusCompleted.equals(i.getCompletionStatus())).count();

        Integer totalPoints = sprintIssueRepository.sumTotalPoints(sprintId);
        Integer completedPoints = sprintIssueRepository.sumCompletedPoints(sprintId);

        int idealRemaining = 0;
        if (sprint.getStartDate() != null && sprint.getEndDate() != null) {
            LocalDate start = sprint.getStartDate().toLocalDate();
            LocalDate end = sprint.getEndDate().toLocalDate();
            try {
                var config = workingDaysService.getDefaultWorkingDaysConfig();
                long totalWorkingDays = workingDaysService.calculateWorkingDays(start, end, mapToWorkingDays(config));
                long remainingWorkingDays = workingDaysService.calculateWorkingDays(today, end, mapToWorkingDays(config));
                idealRemaining = totalWorkingDays > 0 ? (int) ((double) remainingWorkingDays / totalWorkingDays * totalIssues) : 0;
            } catch (Exception e) {
                long daysTotal = ChronoUnit.DAYS.between(start, end) + 1;
                long daysRemaining = ChronoUnit.DAYS.between(today, end);
                idealRemaining = daysTotal > 0 ? (int) ((double) daysRemaining / daysTotal * totalIssues) : 0;
            }
        }

        sprintBurndownRepository.save(SprintBurndown.builder()
            .sprintId(sprintId).snapshotDate(today).totalIssues(totalIssues)
            .completedIssues(completedIssues)
            .remainingPoints(totalPoints != null ? totalPoints - (completedPoints != null ? completedPoints : 0) : 0)
            .idealRemaining(idealRemaining).build());

        // Gap 13: Also capture cumulative flow snapshot
        try {
            captureCumulativeFlowSnapshot(sprint);
        } catch (Exception e) {
            log.warn("Failed to capture CFD snapshot: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Double getAverageVelocity(UUID boardId) {
        return sprintRepository.getAverageVelocity(boardId);
    }

    // Gap 12: Velocity chart with committed vs completed per sprint
    @Transactional(readOnly = true)
    public VelocityChartResponse getVelocityChart(UUID boardId) {
        List<Sprint> closedSprints = sprintRepository.findByBoardConfigIdAndStateOrderBySequence(boardId, sprintStateClosed);

        List<VelocityChartResponse.SprintVelocityEntry> entries = closedSprints.stream()
            .map(s -> VelocityChartResponse.SprintVelocityEntry.builder()
                .sprintId(s.getId()).sprintName(s.getName())
                .startDate(s.getStartDate() != null ? s.getStartDate().toLocalDate() : null)
                .endDate(s.getEndDate() != null ? s.getEndDate().toLocalDate() : null)
                .committedPoints(s.getCommittedPoints() != null ? s.getCommittedPoints() : 0)
                .completedPoints(s.getCompletedPoints() != null ? s.getCompletedPoints() : 0)
                .build())
            .collect(Collectors.toList());

        Double avg = sprintRepository.getAverageVelocity(boardId);

        return VelocityChartResponse.builder()
            .boardId(boardId).averageVelocity(avg != null ? avg : 0.0).sprints(entries).build();
    }

    // ==================== REPORTS (Gap 10) ====================

    @Transactional(readOnly = true)
    public SprintReportResponse getSprintReport(UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));

        List<SprintIssue> allIssues = sprintIssueRepository.findBySprintId(sprintId);

        List<SprintIssueResponse> completed = allIssues.stream()
            .filter(i -> completionStatusCompleted.equals(i.getCompletionStatus()))
            .map(this::toSprintIssueResponse).collect(Collectors.toList());

        List<SprintIssueResponse> notCompleted = allIssues.stream()
            .filter(i -> !completionStatusCompleted.equals(i.getCompletionStatus()) && !completionStatusDropped.equals(i.getCompletionStatus()))
            .filter(i -> i.getRemovedAt() == null)
            .map(this::toSprintIssueResponse).collect(Collectors.toList());

        List<SprintIssueResponse> punted = allIssues.stream()
            .filter(i -> i.getRemovedAt() != null || completionStatusDropped.equals(i.getCompletionStatus()))
            .map(this::toSprintIssueResponse).collect(Collectors.toList());

        List<String> addedDuringSprint = allIssues.stream()
            .filter(i -> sprint.getStartDate() != null && i.getAddedAt() != null
                && i.getAddedAt().isAfter(sprint.getStartDate()))
            .map(i -> i.getIssueId().toString()).collect(Collectors.toList());

        int inProgress = (int) notCompleted.stream()
            .filter(i -> completionStatusInProgress.equals(i.getCompletionStatus())).count();
        int todo = notCompleted.size() - inProgress;

        int committedPts = sprint.getCommittedPoints() != null ? sprint.getCommittedPoints() : 0;
        int completedPts = sprint.getCompletedPoints() != null ? sprint.getCompletedPoints() : 0;

        return SprintReportResponse.builder()
            .sprintId(sprintId).sprintName(sprint.getName()).sprintGoal(sprint.getGoal())
            .startDate(sprint.getStartDate()).endDate(sprint.getEndDate())
            .completeDate(sprint.getCompleteDate()).state(sprint.getState())
            .completedIssues(completed).issuesNotCompletedInCurrentSprint(notCompleted)
            .puntedIssues(punted).issueKeysAddedDuringSprint(addedDuringSprint)
            .committedPoints(committedPts).completedPoints(completedPts)
            .scopeChangePoints(completedPts - committedPts)
            .totalIssues(allIssues.size()).completedIssueCount(completed.size())
            .inProgressIssueCount(inProgress).todoIssueCount(todo)
            .completionRate(allIssues.isEmpty() ? 0 : (double) completed.size() / allIssues.size() * 100)
            .build();
    }

    // ==================== ANALYTICS (Gaps 13, 14, 15) ====================

    // Gap 13: Cumulative flow diagram
    @Transactional(readOnly = true)
    public CumulativeFlowResponse getCumulativeFlowDiagram(UUID boardId, LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();

        List<CumulativeFlowSnapshot> snapshots = cumulativeFlowSnapshotRepository
            .findByBoardIdAndDateRange(boardId, from, to);

        Set<String> columnSet = new LinkedHashSet<>();
        Map<LocalDate, Map<String, Integer>> byDate = new LinkedHashMap<>();

        for (CumulativeFlowSnapshot s : snapshots) {
            columnSet.add(s.getColumnName());
            byDate.computeIfAbsent(s.getSnapshotDate(), k -> new LinkedHashMap<>())
                .put(s.getColumnName(), s.getIssueCount());
        }

        List<CumulativeFlowResponse.CfdDataPoint> dataPoints = byDate.entrySet().stream()
            .map(e -> CumulativeFlowResponse.CfdDataPoint.builder()
                .date(e.getKey()).columnCounts(e.getValue()).build())
            .collect(Collectors.toList());

        return CumulativeFlowResponse.builder()
            .boardId(boardId).columns(new ArrayList<>(columnSet)).dataPoints(dataPoints).build();
    }

    private void captureCumulativeFlowSnapshot(Sprint sprint) {
        BoardConfig board = sprint.getBoardConfig();
        if (board == null || board.getColumns() == null) return;

        List<SprintIssue> activeIssues = sprintIssueRepository.findActiveBySprintId(sprint.getId());
        LocalDate today = LocalDate.now();

        for (BoardColumn column : board.getColumns()) {
            String colName = column.getName();
            long count = activeIssues.stream()
                .filter(si -> {
                    if (si.getPlanItem() == null) return false;
                    String status = si.getCompletionStatus();
                    if (StatusCategoryHelper.isCompleted(colName)) return completionStatusCompleted.equals(status);
                    if (StatusCategoryHelper.isInProgress(colName)) return completionStatusInProgress.equals(status);
                    return completionStatusUncompleted.equals(status);
                }).count();

            cumulativeFlowSnapshotRepository.save(CumulativeFlowSnapshot.builder()
                .boardId(board.getId()).sprintId(sprint.getId())
                .snapshotDate(today).columnName(colName).issueCount((int) count).build());
        }
    }

    // Gap 14: Control chart
    @Transactional(readOnly = true)
    public ControlChartResponse getControlChart(UUID boardId) {
        List<Sprint> closedSprints = sprintRepository.findByBoardConfigIdAndStateOrderBySequence(boardId, sprintStateClosed);
        List<UUID> sprintIds = closedSprints.stream().map(Sprint::getId).collect(Collectors.toList());

        List<SprintIssue> completedIssues = sprintIds.isEmpty() ? List.of() :
            sprintIssueRepository.findBySprintIds(sprintIds).stream()
                .filter(i -> completionStatusCompleted.equals(i.getCompletionStatus()) && i.getCompletedAt() != null && i.getAddedAt() != null)
                .collect(Collectors.toList());

        List<ControlChartResponse.IssueTimingEntry> entries = completedIssues.stream().map(si -> {
            double cycleTime = ChronoUnit.HOURS.between(si.getAddedAt(), si.getCompletedAt()) / 24.0;
            return ControlChartResponse.IssueTimingEntry.builder()
                .issueId(si.getIssueId())
                .planItemId(si.getPlanItem() != null ? si.getPlanItem().getId() : null)
                .cycleTimeDays(Math.max(cycleTime, 0))
                .leadTimeDays(Math.max(cycleTime, 0))
                .completedAt(si.getCompletedAt()).build();
        }).collect(Collectors.toList());

        double avgCycle = entries.stream().mapToDouble(ControlChartResponse.IssueTimingEntry::getCycleTimeDays).average().orElse(0);
        double avgLead = entries.stream().mapToDouble(ControlChartResponse.IssueTimingEntry::getLeadTimeDays).average().orElse(0);

        double variance = entries.stream()
            .mapToDouble(e -> Math.pow(e.getCycleTimeDays() - avgCycle, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);

        return ControlChartResponse.builder()
            .boardId(boardId).averageCycleTime(avgCycle).averageLeadTime(avgLead)
            .standardDeviation(stdDev).issues(entries).build();
    }

    // Gap 15: Epic burndown
    @Transactional(readOnly = true)
    public EpicBurndownResponse getEpicBurndown(UUID epicPlanItemId) {
        PlanItem epic = planItemRepository.findById(epicPlanItemId)
            .orElseThrow(() -> new ResourceNotFoundException("PlanItem", "id", epicPlanItemId));

        List<PlanItem> children = planItemRepository.findByParentId(epicPlanItemId);
        List<UUID> childIds = children.stream().map(PlanItem::getId).collect(Collectors.toList());

        Map<UUID, List<SprintIssue>> issuesBySprint = new HashMap<>();
        for (UUID childId : childIds) {
            List<SprintIssue> links = sprintIssueRepository.findByPlanItemIdAndRemovedAtIsNull(childId);
            for (SprintIssue link : links) {
                issuesBySprint.computeIfAbsent(link.getSprint().getId(), k -> new ArrayList<>()).add(link);
            }
        }

        List<EpicBurndownResponse.EpicSprintEntry> entries = issuesBySprint.entrySet().stream()
            .map(e -> {
                Sprint sprint = e.getValue().get(0).getSprint();
                int total = e.getValue().stream()
                    .mapToInt(si -> si.getPlanItem() != null && si.getPlanItem().getStoryPoints() != null ? si.getPlanItem().getStoryPoints() : 0)
                    .sum();
                int completed = e.getValue().stream()
                    .filter(si -> completionStatusCompleted.equals(si.getCompletionStatus()))
                    .mapToInt(si -> si.getPlanItem() != null && si.getPlanItem().getStoryPoints() != null ? si.getPlanItem().getStoryPoints() : 0)
                    .sum();
                return EpicBurndownResponse.EpicSprintEntry.builder()
                    .sprintId(sprint.getId()).sprintName(sprint.getName())
                    .totalPoints(total).completedPoints(completed).remainingPoints(total - completed).build();
            })
            .collect(Collectors.toList());

        return EpicBurndownResponse.builder()
            .epicId(epicPlanItemId).epicName(epic.getIssueTitle()).sprintEntries(entries).build();
    }

    // ==================== BOARD FEATURES (Gap 18) ====================

    @Transactional(readOnly = true)
    public BoardFeaturesResponse getBoardFeatures(UUID boardId) {
        BoardConfig board = boardConfigRepository.findById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardConfig", "id", boardId));
        return BoardFeaturesResponse.builder()
            .boardId(boardId)
            .sprints(Boolean.TRUE.equals(board.getFeatureSprints()))
            .backlog(Boolean.TRUE.equals(board.getFeatureBacklog()))
            .estimation(Boolean.TRUE.equals(board.getFeatureEstimation()))
            .parallelSprints(Boolean.TRUE.equals(board.getFeatureParallelSprints()))
            .build();
    }

    @Transactional
    public BoardFeaturesResponse updateBoardFeatures(UUID boardId, BoardFeaturesRequest request) {
        BoardConfig board = boardConfigRepository.findById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardConfig", "id", boardId));

        if (request.getSprints() != null) board.setFeatureSprints(request.getSprints());
        if (request.getBacklog() != null) board.setFeatureBacklog(request.getBacklog());
        if (request.getEstimation() != null) board.setFeatureEstimation(request.getEstimation());
        if (request.getParallelSprints() != null) board.setFeatureParallelSprints(request.getParallelSprints());

        boardConfigRepository.save(board);
        return getBoardFeatures(boardId);
    }

    // ==================== BACKLOG PLANNING (Gap 22) ====================

    @Transactional(readOnly = true)
    public BacklogPlanningResponse getBacklogPlanningView(UUID boardId) {
        BoardConfig board = boardConfigRepository.findById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardConfig", "id", boardId));

        List<Sprint> sprints = sprintRepository.findByBoardConfigIdOrderBySequenceAsc(boardId);

        List<BacklogPlanningResponse.SprintBacklogSection> sections = sprints.stream()
            .filter(s -> !sprintStateAbandoned.equals(s.getState()))
            .map(sprint -> {
                List<SprintIssue> issues = sprintIssueRepository.findActiveBySprintId(sprint.getId());
                int totalPoints = issues.stream()
                    .mapToInt(i -> i.getPlanItem() != null && i.getPlanItem().getStoryPoints() != null ? i.getPlanItem().getStoryPoints() : 0)
                    .sum();
                return BacklogPlanningResponse.SprintBacklogSection.builder()
                    .sprintId(sprint.getId()).sprintName(sprint.getName()).sprintState(sprint.getState())
                    .totalIssues(issues.size()).totalPoints(totalPoints)
                    .issues(issues.stream().map(this::toSprintIssueResponse).collect(Collectors.toList()))
                    .build();
            }).collect(Collectors.toList());

        // Backlog: plan items not in any active sprint
        Set<UUID> assignedPlanItemIds = new HashSet<>();
        for (BacklogPlanningResponse.SprintBacklogSection section : sections) {
            section.getIssues().forEach(i -> { if (i.getPlanItemId() != null) assignedPlanItemIds.add(i.getPlanItemId()); });
        }

        UUID planId = board.getPlan() != null ? board.getPlan().getId() : null;
        List<UUID> backlogItemIds = List.of();
        int backlogPoints = 0;
        if (planId != null) {
            List<PlanItem> allItems = planItemRepository.findByPlanIdAndIsActiveTrue(planId);
            List<PlanItem> unassigned = allItems.stream()
                .filter(item -> !assignedPlanItemIds.contains(item.getId()))
                .collect(Collectors.toList());
            backlogItemIds = unassigned.stream().map(PlanItem::getId).collect(Collectors.toList());
            backlogPoints = unassigned.stream()
                .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0).sum();
        }

        return BacklogPlanningResponse.builder()
            .boardId(boardId).sprintSections(sections)
            .backlog(BacklogPlanningResponse.BacklogSection.builder()
                .totalIssues(backlogItemIds.size()).totalPoints(backlogPoints)
                .planItemIds(backlogItemIds).build())
            .build();
    }

    // ==================== HELPERS ====================

    private void recordSprintEvent(UUID sprintId, String eventType, UUID planItemId,
                                   Integer oldValue, Integer newValue, Integer pointsDelta, UUID userId) {
        try {
            sprintEventRepository.save(SprintEvent.builder()
                .sprintId(sprintId).eventType(eventType).planItemId(planItemId)
                .oldValue(oldValue).newValue(newValue).pointsDelta(pointsDelta)
                .eventTimestamp(LocalDateTime.now()).userId(userId).build());
        } catch (Exception e) {
            log.warn("Failed to record sprint event: {}", e.getMessage());
        }
    }

    private String calculateRankBetween(String after, String before) {
        if (after == null && before == null) return defaultRankValue;
        if (after == null) return before + "0";
        if (before == null) return after + "z";

        if (after.compareTo(before) < 0) {
            int midPoint = (after.charAt(after.length() - 1) + before.charAt(0)) / 2;
            return after.substring(0, after.length() - 1) + (char) midPoint;
        }
        return after + "m";
    }

    private WorkingDays mapToWorkingDays(com.avionics_systems.plan.dto.response.WorkingDaysResponse config) {
        return WorkingDays.builder()
            .id(config.getId()).monday(config.getMonday()).tuesday(config.getTuesday())
            .wednesday(config.getWednesday()).thursday(config.getThursday())
            .friday(config.getFriday()).saturday(config.getSaturday())
            .sunday(config.getSunday()).hoursPerDay(config.getHoursPerDay()).build();
    }

    private void createAuditLog(UUID sprintId, String eventType, UUID userId, Map<String, String> details) {
        String detailsJson = null;
        if (details != null) {
            try {
                detailsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(details);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) { /* ignore */ }
        }
        sprintAuditLogRepository.save(SprintAuditLog.builder()
            .sprintId(sprintId).eventType(eventType).userId(userId).details(detailsJson).build());
    }

    private SprintResponse toResponse(Sprint sprint) {
        List<SprintIssue> issues = sprintIssueRepository.findBySprintId(sprint.getId());
        return toResponseWithIssues(sprint, issues);
    }

    private SprintResponse toResponseWithIssues(Sprint sprint, List<SprintIssue> issues) {
        int totalIssues = issues.size();
        int completedIssues = (int) issues.stream()
            .filter(i -> completionStatusCompleted.equals(i.getCompletionStatus())).count();

        return SprintResponse.builder()
            .id(sprint.getId())
            .boardId(sprint.getBoardConfig() != null ? sprint.getBoardConfig().getId() : null)
            .name(sprint.getName()).goal(sprint.getGoal())
            .startDate(sprint.getStartDate()).endDate(sprint.getEndDate())
            .completeDate(sprint.getCompleteDate()).state(sprint.getState())
            .sequence(sprint.getSequence()).velocity(sprint.getVelocity())
            .wipLimit(sprint.getWipLimit())
            .committedPoints(sprint.getCommittedPoints()).completedPoints(sprint.getCompletedPoints())
            .totalIssues(totalIssues).completedIssues(completedIssues)
            .createdAt(sprint.getCreatedAt()).updatedAt(sprint.getUpdatedAt()).build();
    }

    private SprintIssueResponse toSprintIssueResponse(SprintIssue issue) {
        return SprintIssueResponse.builder()
            .id(issue.getId())
            .sprintId(issue.getSprint().getId())
            .planItemId(issue.getPlanItem() != null ? issue.getPlanItem().getId() : null)
            .issueId(issue.getIssueId())
            .rankValue(issue.getRankValue())
            .addedAt(issue.getAddedAt()).addedBy(issue.getAddedBy())
            .removedAt(issue.getRemovedAt())
            .completionStatus(issue.getCompletionStatus())
            .completedAt(issue.getCompletedAt())
            .flagged(issue.getFlagged())
            .flagReason(issue.getFlagReason())
            .build();
    }
}
