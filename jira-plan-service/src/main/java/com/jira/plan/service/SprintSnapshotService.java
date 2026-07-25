package com.jira.plan.service;

import com.jira.plan.entity.Sprint;
import com.jira.plan.entity.SprintIssue;
import com.jira.plan.entity.SprintSnapshot;
import com.jira.plan.entity.VelocityHistory;
import com.jira.plan.repository.SprintIssueRepository;
import com.jira.plan.repository.SprintSnapshotRepository;
import com.jira.plan.repository.VelocityHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Sprint Snapshot Service - automates sprint snapshots for velocity and burndown charts.
 * Captures COMMITMENT snapshots on sprint start, DAILY snapshots for burndown,
 * and CLOSURE snapshots on sprint completion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SprintSnapshotService {

    private final SprintSnapshotRepository sprintSnapshotRepository;
    private final VelocityHistoryRepository velocityHistoryRepository;
    private final SprintIssueRepository sprintIssueRepository;

    @Value("${app.sprint.completion-status.completed:COMPLETED}")
    private String completionStatusCompleted;

    /**
     * Record a COMMITMENT snapshot when sprint starts.
     * This captures the baseline story points at sprint start.
     */
    @Transactional
    public SprintSnapshot recordCommitmentSnapshot(Sprint sprint) {
        String sprintId = sprint.getId().toString();
        String boardId = sprint.getBoardConfig().getId().toString();

        // Check if snapshot already exists
        if (sprintSnapshotRepository.existsBySprintIdAndSnapshotType(sprintId, SprintSnapshot.SnapshotType.COMMITMENT)) {
            log.debug("Commitment snapshot already exists for sprint {}", sprintId);
            return sprintSnapshotRepository.findFirstBySprintIdAndSnapshotType(sprintId, SprintSnapshot.SnapshotType.COMMITMENT)
                    .orElse(null);
        }

        // Calculate sprint stats at commitment time
        List<SprintIssue> issues = sprintIssueRepository.findBySprintId(UUID.fromString(sprintId));

        int totalIssues = issues.size();
        int completedIssues = 0;
        BigDecimal totalPoints = BigDecimal.ZERO;
        BigDecimal completedPoints = BigDecimal.ZERO;

        for (SprintIssue issue : issues) {
            // Get story points from plan item or issue
            BigDecimal points = getIssuePoints(issue);
            if (points != null) {
                totalPoints = totalPoints.add(points);
            }
        }

        SprintSnapshot snapshot = SprintSnapshot.builder()
                .sprintId(sprintId)
                .boardId(boardId)
                .snapshotType(SprintSnapshot.SnapshotType.COMMITMENT)
                .recordDate(LocalDate.now())
                .totalIssues(totalIssues)
                .completedIssues(0)
                .totalPoints(totalPoints)
                .completedPoints(BigDecimal.ZERO)
                .remainingPoints(totalPoints)
                .originalPoints(totalPoints)
                .scopeChangePoints(BigDecimal.ZERO)
                .build();

        snapshot = sprintSnapshotRepository.save(snapshot);
        log.info("Recorded commitment snapshot for sprint {}: {} issues, {} points",
                sprint.getName(), totalIssues, totalPoints);

        return snapshot;
    }

    /**
     * Record a DAILY snapshot for burndown tracking.
     * This should be called once per day during the sprint.
     */
    @Transactional
    public SprintSnapshot recordDailySnapshot(Sprint sprint) {
        String sprintId = sprint.getId().toString();
        String boardId = sprint.getBoardConfig().getId().toString();
        LocalDate today = LocalDate.now();

        List<SprintIssue> issues = sprintIssueRepository.findBySprintId(UUID.fromString(sprintId));
        List<SprintIssue> activeIssues = sprintIssueRepository.findActiveBySprintId(UUID.fromString(sprintId));

        int totalIssues = issues.size();
        int completedIssues = (int) activeIssues.stream()
                .filter(i -> completionStatusCompleted.equals(i.getCompletionStatus()))
                .count();

        BigDecimal totalPoints = BigDecimal.ZERO;
        BigDecimal completedPoints = BigDecimal.ZERO;
        BigDecimal remainingPoints = BigDecimal.ZERO;

        for (SprintIssue issue : issues) {
            BigDecimal points = getIssuePoints(issue);
            if (points != null) {
                totalPoints = totalPoints.add(points);
                if (completionStatusCompleted.equals(issue.getCompletionStatus())) {
                    completedPoints = completedPoints.add(points);
                }
            }
        }
        remainingPoints = totalPoints.subtract(completedPoints);

        // Get original commitment
        BigDecimal originalPoints = sprintSnapshotRepository.findFirstBySprintIdAndSnapshotType(sprintId, SprintSnapshot.SnapshotType.COMMITMENT)
                .map(SprintSnapshot::getOriginalPoints)
                .orElse(totalPoints);

        BigDecimal scopeChange = totalPoints.subtract(originalPoints);

        SprintSnapshot snapshot = SprintSnapshot.builder()
                .sprintId(sprintId)
                .boardId(boardId)
                .snapshotType(SprintSnapshot.SnapshotType.DAILY)
                .recordDate(today)
                .totalIssues(totalIssues)
                .completedIssues(completedIssues)
                .totalPoints(totalPoints)
                .completedPoints(completedPoints)
                .remainingPoints(remainingPoints)
                .originalPoints(originalPoints)
                .scopeChangePoints(scopeChange)
                .build();

        snapshot = sprintSnapshotRepository.save(snapshot);
        log.debug("Recorded daily snapshot for sprint {} on {}: {} remaining points",
                sprint.getName(), today, remainingPoints);

        return snapshot;
    }

    /**
     * Record a CLOSURE snapshot when sprint completes.
     */
    @Transactional
    public SprintSnapshot recordClosureSnapshot(Sprint sprint, int completedIssues, BigDecimal completedPoints) {
        String sprintId = sprint.getId().toString();
        String boardId = sprint.getBoardConfig().getId().toString();

        List<SprintIssue> issues = sprintIssueRepository.findBySprintId(UUID.fromString(sprintId));
        int totalIssues = issues.size();

        // Get original commitment
        BigDecimal originalPoints = sprintSnapshotRepository.findFirstBySprintIdAndSnapshotType(sprintId, SprintSnapshot.SnapshotType.COMMITMENT)
                .map(SprintSnapshot::getOriginalPoints)
                .orElse(completedPoints);

        BigDecimal scopeChange = BigDecimal.ZERO;
        BigDecimal totalPoints = completedPoints;
        for (SprintIssue issue : issues) {
            BigDecimal points = getIssuePoints(issue);
            if (points != null) {
                totalPoints = totalPoints.add(points);
            }
        }
        scopeChange = totalPoints.subtract(originalPoints);

        SprintSnapshot snapshot = SprintSnapshot.builder()
                .sprintId(sprintId)
                .boardId(boardId)
                .snapshotType(SprintSnapshot.SnapshotType.CLOSURE)
                .recordDate(LocalDate.now())
                .totalIssues(totalIssues)
                .completedIssues(completedIssues)
                .totalPoints(totalPoints)
                .completedPoints(completedPoints)
                .remainingPoints(BigDecimal.ZERO)
                .originalPoints(originalPoints)
                .scopeChangePoints(scopeChange)
                .build();

        snapshot = sprintSnapshotRepository.save(snapshot);

        // Also record velocity history
        recordVelocityHistory(sprint, completedPoints, totalIssues);

        log.info("Recorded closure snapshot for sprint {}: {} points completed", sprint.getName(), completedPoints);

        return snapshot;
    }

    /**
     * Record velocity history for a completed sprint.
     */
    @Transactional
    public void recordVelocityHistory(Sprint sprint, BigDecimal completedPoints, int issueCount) {
        String boardId = sprint.getBoardConfig().getId().toString();
        String sprintId = sprint.getId().toString();

        VelocityHistory history = VelocityHistory.builder()
                .boardId(boardId)
                .sprintId(sprintId)
                .sprintName(sprint.getName())
                .sprintStartDate(sprint.getStartDate() != null ? sprint.getStartDate().toLocalDate() : null)
                .sprintEndDate(sprint.getEndDate() != null ? sprint.getEndDate().toLocalDate() : null)
                .committedPoints(sprint.getCommittedPoints() != null ? BigDecimal.valueOf(sprint.getCommittedPoints()) : BigDecimal.ZERO)
                .completedPoints(completedPoints)
                .velocity(completedPoints != null ? completedPoints.intValue() : 0)
                .issueCount(issueCount)
                .build();

        velocityHistoryRepository.save(history);
        log.debug("Recorded velocity {} for board {}", completedPoints, boardId);
    }

    /**
     * Get average velocity for a board.
     */
    @Transactional(readOnly = true)
    public Double getAverageVelocity(String boardId) {
        return velocityHistoryRepository.getAverageVelocity(boardId);
    }

    /**
     * Get velocity history for a board.
     */
    @Transactional(readOnly = true)
    public List<VelocityHistory> getVelocityHistory(String boardId) {
        return velocityHistoryRepository.findByBoardIdOrderByCreatedAtDesc(boardId);
    }

    /**
     * Get sprint snapshots for burndown chart.
     */
    @Transactional(readOnly = true)
    public List<SprintSnapshot> getSprintSnapshots(String sprintId) {
        return sprintSnapshotRepository.findBySprintIdOrderByRecordDateAsc(sprintId);
    }

    /**
     * Scheduled job to capture daily snapshots for all active sprints.
     * Runs at 1:00 AM every day.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @SchedulerLock(name = "SprintSnapshotService_captureDailySnapshots", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    @Transactional
    public void captureDailySnapshotsForActiveSprints() {
        log.info("Running scheduled daily snapshot capture");
        // This would query all ACTIVE sprints and record daily snapshots
        // Implementation would depend on finding active sprints across all boards
        // For now, this is a placeholder that can be wired up
    }

    private BigDecimal getIssuePoints(SprintIssue issue) {
        // Try to get points from plan item first
        if (issue.getPlanItem() != null && issue.getPlanItem().getStoryPoints() != null) {
            return BigDecimal.valueOf(issue.getPlanItem().getStoryPoints());
        }

        // Points from story points field
        // This would typically be fetched from the issue-service
        return null;
    }
}