package com.jira.issue.service;

import com.jira.cluster.util.StatusCategoryHelper;
import com.jira.issue.dto.*;
import com.jira.issue.entity.*;
import com.jira.issue.exception.ResourceNotFoundException;
import com.jira.issue.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Epic Management Service - Jira DC compliant epic tracking system.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EpicService {

    private final EpicRepository epicRepository;
    private final EpicIssueRepository epicIssueRepository;
    private final EpicProgressHistoryRepository epicProgressHistoryRepository;
    private final IssueRepository issueRepository;

    @Value("${app.defaults.epic-color:#0052CC}")
    private String defaultEpicColor;

    @Value("${app.defaults.epic-status:OPEN}")
    private String defaultEpicStatus;

    @Transactional
    public EpicResponse createEpic(CreateEpicRequest request) {
        Epic epic = Epic.builder()
                .name(request.getName())
                .summary(request.getSummary())
                .description(request.getDescription())
                .color(request.getColor() != null ? request.getColor() : defaultEpicColor)
                .leadId(request.getLeadId())
                .leadName(request.getLeadName())
                .status(request.getStatus() != null ? request.getStatus() : defaultEpicStatus)
                .linkedIssueId(request.getLinkedIssueId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        epic = epicRepository.save(epic);
        log.info("Created epic: {} ({})", epic.getName(), epic.getId());

        return toResponse(epic);
    }

    @Transactional(readOnly = true)
    public EpicResponse getEpic(String epicId) {
        Epic epic = epicRepository.findById(epicId)
                .orElseThrow(() -> new ResourceNotFoundException("Epic", "id", epicId));
        return toResponse(epic);
    }

    @Transactional(readOnly = true)
    public List<EpicResponse> getAllEpics() {
        return epicRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EpicResponse> getEpicsByLead(String userId) {
        return epicRepository.findByLeadId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EpicResponse> getEpicsByStatus(String status) {
        return epicRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EpicResponse updateEpic(String epicId, UpdateEpicRequest request) {
        Epic epic = epicRepository.findById(epicId)
                .orElseThrow(() -> new ResourceNotFoundException("Epic", "id", epicId));

        if (request.getName() != null) epic.setName(request.getName());
        if (request.getSummary() != null) epic.setSummary(request.getSummary());
        if (request.getDescription() != null) epic.setDescription(request.getDescription());
        if (request.getColor() != null) epic.setColor(request.getColor());
        if (request.getLeadId() != null) epic.setLeadId(request.getLeadId());
        if (request.getLeadName() != null) epic.setLeadName(request.getLeadName());
        if (request.getStatus() != null) epic.setStatus(request.getStatus());
        if (request.getStartDate() != null) epic.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) epic.setEndDate(request.getEndDate());

        epic = epicRepository.save(epic);
        log.info("Updated epic: {}", epicId);

        return toResponse(epic);
    }

    @Transactional
    public void deleteEpic(String epicId) {
        Epic epic = epicRepository.findById(epicId)
                .orElseThrow(() -> new ResourceNotFoundException("Epic", "id", epicId));

        // EpicIssues will be cascade deleted due to FK constraint
        epicRepository.delete(epic);
        log.info("Deleted epic: {}", epicId);
    }

    @Transactional
    public void addIssueToEpic(String epicId, String issueId) {
        // Verify epic exists
        if (!epicRepository.existsById(epicId)) {
            throw new ResourceNotFoundException("Epic", "id", epicId);
        }

        // Check if issue already in epic
        if (epicIssueRepository.existsByEpicIdAndIssueId(epicId, issueId)) {
            throw new IllegalArgumentException("Issue already in epic");
        }

        EpicIssue epicIssue = EpicIssue.builder()
                .epicId(epicId)
                .issueId(issueId)
                .build();

        epicIssueRepository.save(epicIssue);

        // Update epic progress
        recalculateEpicProgress(epicId);

        log.info("Added issue {} to epic {}", issueId, epicId);
    }

    @Transactional
    public void removeIssueFromEpic(String epicId, String issueId) {
        EpicIssue epicIssue = epicIssueRepository.findByEpicIdAndIssueId(epicId, issueId)
                .orElseThrow(() -> new ResourceNotFoundException("EpicIssue", "epicId+issueId", epicId + "+" + issueId));

        epicIssueRepository.delete(epicIssue);

        // Update epic progress
        recalculateEpicProgress(epicId);

        log.info("Removed issue {} from epic {}", issueId, epicId);
    }

    @Transactional(readOnly = true)
    public List<String> getEpicIssueIds(String epicId) {
        return epicIssueRepository.findIssueIdsByEpicId(epicId);
    }

    @Transactional
    public void recalculateEpicProgress(String epicId) {
        List<String> issueIds = epicIssueRepository.findIssueIdsByEpicId(epicId);

        AtomicReference<BigDecimal> totalPoints = new AtomicReference<>(BigDecimal.ZERO);
        AtomicReference<BigDecimal> completedPoints = new AtomicReference<>(BigDecimal.ZERO);
        int totalIssues = issueIds.size();
        AtomicReference<Integer> completedIssues = new AtomicReference<>(0);

        for (String issueId : issueIds) {
            try {
                UUID issueUuid = UUID.fromString(issueId);
                issueRepository.findById(issueUuid).ifPresent(issue -> {
                    if (issue.getStoryPoints() != null) {
                        totalPoints.updateAndGet(v -> v.add(BigDecimal.valueOf(issue.getStoryPoints())));
                    }
                });
            } catch (IllegalArgumentException e) {
                log.warn("Invalid issue ID format: {}", issueId);
            }
        }

        // Count completed issues using StatusCategoryHelper
        for (String issueId : issueIds) {
            try {
                UUID issueUuid = UUID.fromString(issueId);
                issueRepository.findById(issueUuid).ifPresent(issue -> {
                    String statusName = issue.getStatus() != null ? issue.getStatus().getName() : "";
                    if (StatusCategoryHelper.isCompleted(statusName)) {
                        completedIssues.updateAndGet(v -> v + 1);
                        if (issue.getStoryPoints() != null) {
                            completedPoints.updateAndGet(v -> v.add(BigDecimal.valueOf(issue.getStoryPoints())));
                        }
                    }
                });
            } catch (IllegalArgumentException e) {
                log.warn("Invalid issue ID format: {}", issueId);
            }
        }

        epicRepository.updateProgress(epicId, totalPoints.get(), completedPoints.get(), totalIssues, completedIssues.get());
        log.debug("Recalculated epic {} progress: {} total points, {} completed", epicId, totalPoints.get(), completedPoints.get());
    }

    @Transactional
    public void recordEpicProgress(String epicId) {
        Epic epic = epicRepository.findById(epicId)
                .orElseThrow(() -> new ResourceNotFoundException("Epic", "id", epicId));

        LocalDate today = LocalDate.now();

        // Skip if already recorded today
        if (epicProgressHistoryRepository.existsByEpicIdAndRecordDate(epicId, today)) {
            return;
        }

        BigDecimal percentComplete = epic.getProgressPercentage();

        EpicProgressHistory history = EpicProgressHistory.builder()
                .epicId(epicId)
                .recordDate(today)
                .totalPoints(epic.getTotalStoryPoints())
                .completedPoints(epic.getCompletedStoryPoints())
                .totalIssues(epic.getTotalIssueCount())
                .completedIssues(epic.getCompletedIssueCount())
                .percentComplete(percentComplete)
                .build();

        epicProgressHistoryRepository.save(history);
        log.debug("Recorded progress history for epic {} on {}", epicId, today);
    }

    @Transactional(readOnly = true)
    public List<EpicProgressResponse> getEpicProgressHistory(String epicId) {
        Epic epic = epicRepository.findById(epicId)
                .orElseThrow(() -> new ResourceNotFoundException("Epic", "id", epicId));

        return epicProgressHistoryRepository.findByEpicIdOrderByRecordDateAsc(epicId).stream()
                .map(h -> EpicProgressResponse.builder()
                        .epicId(epicId)
                        .epicName(epic.getName())
                        .totalStoryPoints(h.getTotalPoints())
                        .completedStoryPoints(h.getCompletedPoints())
                        .progressPercentage(h.getPercentComplete())
                        .totalIssueCount(h.getTotalIssues())
                        .completedIssueCount(h.getCompletedIssues())
                        .remainingIssueCount(h.getTotalIssues() - h.getCompletedIssues())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EpicProgressResponse getCurrentEpicProgress(String epicId) {
        Epic epic = epicRepository.findById(epicId)
                .orElseThrow(() -> new ResourceNotFoundException("Epic", "id", epicId));

        BigDecimal progressPercent = epic.getProgressPercentage();

        // Estimate completion based on current velocity
        LocalDate estimatedCompletion = null;
        if (epic.getEndDate() != null && progressPercent.compareTo(BigDecimal.ZERO) > 0) {
            // Simple linear estimation
            BigDecimal remainingPercent = BigDecimal.valueOf(100).subtract(progressPercent);
            long totalDays = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), epic.getEndDate());
            if (totalDays > 0) {
                long estimatedRemainingDays = remainingPercent.multiply(BigDecimal.valueOf(totalDays))
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING).longValue();
                estimatedCompletion = LocalDate.now().plusDays(estimatedRemainingDays);
            }
        }

        return EpicProgressResponse.builder()
                .epicId(epicId)
                .epicName(epic.getName())
                .totalStoryPoints(epic.getTotalStoryPoints())
                .completedStoryPoints(epic.getCompletedStoryPoints())
                .progressPercentage(progressPercent)
                .totalIssueCount(epic.getTotalIssueCount())
                .completedIssueCount(epic.getCompletedIssueCount())
                .remainingIssueCount(epic.getTotalIssueCount() - epic.getCompletedIssueCount())
                .estimatedCompletion(estimatedCompletion)
                .build();
    }

    @Transactional
    public void updateEpicStatus(String epicId, String newStatus) {
        Epic epic = epicRepository.findById(epicId)
                .orElseThrow(() -> new ResourceNotFoundException("Epic", "id", epicId));

        epic.setStatus(newStatus);
        epicRepository.save(epic);

        // Record progress snapshot
        recordEpicProgress(epicId);

        log.info("Updated epic {} status to {}", epicId, newStatus);
    }

    private EpicResponse toResponse(Epic epic) {
        return EpicResponse.builder()
                .id(epic.getId())
                .name(epic.getName())
                .summary(epic.getSummary())
                .description(epic.getDescription())
                .color(epic.getColor())
                .leadId(epic.getLeadId())
                .leadName(epic.getLeadName())
                .status(epic.getStatus())
                .startDate(epic.getStartDate())
                .endDate(epic.getEndDate())
                .linkedIssueId(epic.getLinkedIssueId())
                .totalStoryPoints(epic.getTotalStoryPoints())
                .completedStoryPoints(epic.getCompletedStoryPoints())
                .totalIssueCount(epic.getTotalIssueCount())
                .completedIssueCount(epic.getCompletedIssueCount())
                .progressPercentage(epic.getProgressPercentage())
                .createdAt(epic.getCreatedAt())
                .updatedAt(epic.getUpdatedAt())
                .build();
    }
}