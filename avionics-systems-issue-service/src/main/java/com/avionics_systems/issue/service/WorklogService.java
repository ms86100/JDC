package com.avionics_systems.issue.service;

import com.avionics_systems.issue.dto.RemainingEstimateStrategy;
import com.avionics_systems.issue.dto.WorklogRequest;
import com.avionics_systems.issue.dto.WorklogResponse;
import com.avionics_systems.issue.entity.Issue;
import com.avionics_systems.issue.entity.Worklog;
import com.avionics_systems.issue.exception.ResourceNotFoundException;
import com.avionics_systems.issue.repository.IssueRepository;
import com.avionics_systems.issue.repository.WorklogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorklogService {

    private final WorklogRepository worklogRepository;
    private final IssueRepository issueRepository;

    @Value("${avionics-systems.services.user-url:http://localhost:8082}")
    private String userServiceUrl;

    @Transactional
    public WorklogResponse createWorklog(WorklogRequest request) {
        Issue issue = issueRepository.findById(request.getIssueId())
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + request.getIssueId()));

        long timeSpent = safeUnbox(request.getTimeSpentSeconds());

        Worklog worklog = Worklog.builder()
                .issueId(request.getIssueId())
                .authorId(request.getAuthorId())
                .startedAt(request.getStartedAt() != null ? request.getStartedAt() : LocalDateTime.now())
                .timeSpentSeconds(timeSpent)
                .timeSpentDisplay(formatTimeDisplay(timeSpent))
                .workDescription(request.getWorkDescription())
                .visibility(request.getVisibility())
                .visibilityGroupId(request.getVisibilityGroupId())
                .build();

        worklog = worklogRepository.save(worklog);

        long currentTimeSpent = safeUnbox(issue.getTimeSpent());
        issue.setTimeSpent(currentTimeSpent + timeSpent);

        RemainingEstimateStrategy strategy = request.getAdjustEstimate() != null
                ? request.getAdjustEstimate() : RemainingEstimateStrategy.AUTO;
        applyRemainingEstimateStrategy(issue, strategy, timeSpent, request.getAdjustmentSeconds());
        issueRepository.save(issue);

        log.info("Created worklog {} for issue {} ({}s logged)", worklog.getId(), request.getIssueId(), timeSpent);
        return toResponse(worklog);
    }

    @Transactional(readOnly = true)
    public List<WorklogResponse> getWorklogsByIssue(UUID issueId) {
        return worklogRepository.findByIssueIdOrderByStartedAtDesc(issueId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorklogResponse> getVisibleWorklogsByIssue(UUID issueId, UUID userId) {
        return worklogRepository.findByIssueIdOrderByStartedAtDesc(issueId)
                .stream()
                .filter(w -> w.getVisibility() == null || w.getVisibility().isEmpty()
                        || Objects.equals(w.getAuthorId(), userId))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorklogResponse getWorklog(UUID worklogId) {
        Worklog worklog = worklogRepository.findById(worklogId)
                .orElseThrow(() -> new ResourceNotFoundException("Worklog not found: " + worklogId));
        return toResponse(worklog);
    }

    @Transactional
    public WorklogResponse updateWorklog(UUID worklogId, UUID expectedIssueId, WorklogRequest request) {
        Worklog worklog = worklogRepository.findById(worklogId)
                .orElseThrow(() -> new ResourceNotFoundException("Worklog not found: " + worklogId));

        if (expectedIssueId != null && !worklog.getIssueId().equals(expectedIssueId)) {
            throw new ResourceNotFoundException("Worklog " + worklogId + " does not belong to issue " + expectedIssueId);
        }

        UUID issueIdForLookup = worklog.getIssueId();
        Issue issue = issueRepository.findById(issueIdForLookup)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueIdForLookup));

        long oldTimeSpent = safeUnbox(worklog.getTimeSpentSeconds());
        long newTimeSpent = safeUnbox(request.getTimeSpentSeconds());
        long delta = newTimeSpent - oldTimeSpent;

        worklog.setTimeSpentSeconds(newTimeSpent);
        worklog.setTimeSpentDisplay(formatTimeDisplay(newTimeSpent));
        worklog.setWorkDescription(request.getWorkDescription());
        if (request.getStartedAt() != null) {
            worklog.setStartedAt(request.getStartedAt());
        }
        if (request.getVisibility() != null) {
            worklog.setVisibility(request.getVisibility());
        }
        if (request.getVisibilityGroupId() != null) {
            worklog.setVisibilityGroupId(request.getVisibilityGroupId());
        }

        Worklog savedWorklog = worklogRepository.save(worklog);

        RemainingEstimateStrategy strategy = request.getAdjustEstimate() != null
                ? request.getAdjustEstimate() : RemainingEstimateStrategy.AUTO;

        if (delta != 0 || strategy == RemainingEstimateStrategy.SET) {
            if (delta != 0) {
                long currentIssueTimeSpent = safeUnbox(issue.getTimeSpent());
                issue.setTimeSpent(Math.max(0, currentIssueTimeSpent + delta));
            }
            applyRemainingEstimateStrategy(issue, strategy, delta, request.getAdjustmentSeconds());
            issueRepository.save(issue);
        }

        log.info("Updated worklog {}", worklogId);
        return toResponse(savedWorklog);
    }

    @Transactional
    public void deleteWorklog(UUID worklogId, UUID expectedIssueId,
                              RemainingEstimateStrategy strategy, Long adjustmentSeconds) {
        Worklog worklog = worklogRepository.findById(worklogId)
                .orElseThrow(() -> new ResourceNotFoundException("Worklog not found: " + worklogId));

        if (expectedIssueId != null && !worklog.getIssueId().equals(expectedIssueId)) {
            throw new ResourceNotFoundException("Worklog " + worklogId + " does not belong to issue " + expectedIssueId);
        }

        Issue issue = issueRepository.findById(worklog.getIssueId())
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + worklog.getIssueId()));

        long removedSeconds = safeUnbox(worklog.getTimeSpentSeconds());

        worklogRepository.deleteById(worklogId);

        long currentTimeSpent = safeUnbox(issue.getTimeSpent());
        issue.setTimeSpent(Math.max(0, currentTimeSpent - removedSeconds));

        if (strategy == null) {
            strategy = RemainingEstimateStrategy.AUTO;
        }

        switch (strategy) {
            case AUTO -> {
                long currentRemaining = safeUnbox(issue.getRemainingEstimate());
                if (currentRemaining > 0 || issue.getOriginalEstimate() != null) {
                    issue.setRemainingEstimate(currentRemaining + removedSeconds);
                }
            }
            case LEAVE -> { /* no change */ }
            case SET -> {
                if (adjustmentSeconds != null) {
                    issue.setRemainingEstimate(adjustmentSeconds);
                }
            }
            case REDUCE -> {
                long currentRemaining = safeUnbox(issue.getRemainingEstimate());
                long reduction = adjustmentSeconds != null ? adjustmentSeconds : removedSeconds;
                issue.setRemainingEstimate(Math.max(0, currentRemaining - reduction));
            }
            case INCREASE -> {
                long currentRemaining = safeUnbox(issue.getRemainingEstimate());
                long increase = adjustmentSeconds != null ? adjustmentSeconds : removedSeconds;
                issue.setRemainingEstimate(currentRemaining + increase);
            }
        }

        issueRepository.save(issue);
        log.info("Deleted worklog {} and adjusted remaining estimate (strategy={})", worklogId, strategy);
    }

    @Transactional
    public void deleteWorklog(UUID worklogId) {
        deleteWorklog(worklogId, null, RemainingEstimateStrategy.AUTO, null);
    }

    @Transactional(readOnly = true)
    public Long getTotalTimeWorked(UUID issueId) {
        Long total = worklogRepository.getTotalTimeSpent(issueId);
        return total != null ? total : 0L;
    }

    @Transactional(readOnly = true)
    public Worklog getWorklogEntity(UUID worklogId) {
        return worklogRepository.findById(worklogId)
                .orElseThrow(() -> new ResourceNotFoundException("Worklog not found: " + worklogId));
    }

    @Transactional(readOnly = true)
    public long[] getAggregateTimeForIssue(UUID issueId) {
        Issue parent = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));
        List<Issue> subtasks = issueRepository.findByParentIssueId(issueId);

        long aggEstimate = safeUnbox(parent.getOriginalEstimate());
        long aggSpent = safeUnbox(parent.getTimeSpent());
        long aggRemaining = safeUnbox(parent.getRemainingEstimate());

        for (Issue sub : subtasks) {
            aggEstimate += safeUnbox(sub.getOriginalEstimate());
            aggSpent += safeUnbox(sub.getTimeSpent());
            aggRemaining += safeUnbox(sub.getRemainingEstimate());
        }

        return new long[]{aggEstimate, aggSpent, aggRemaining};
    }

    private void applyRemainingEstimateStrategy(Issue issue, RemainingEstimateStrategy strategy,
                                                 long timeSpentDelta, Long adjustmentSeconds) {
        switch (strategy) {
            case AUTO -> {
                if (issue.getRemainingEstimate() != null) {
                    issue.setRemainingEstimate(Math.max(0, issue.getRemainingEstimate() - timeSpentDelta));
                } else if (issue.getOriginalEstimate() != null) {
                    issue.setRemainingEstimate(Math.max(0, issue.getOriginalEstimate() - timeSpentDelta));
                }
            }
            case LEAVE -> { /* no change */ }
            case SET -> {
                if (adjustmentSeconds != null) {
                    issue.setRemainingEstimate(adjustmentSeconds);
                }
            }
            case REDUCE -> {
                long current = safeUnbox(issue.getRemainingEstimate());
                long reduction = adjustmentSeconds != null ? adjustmentSeconds : timeSpentDelta;
                issue.setRemainingEstimate(Math.max(0, current - reduction));
            }
            case INCREASE -> {
                long current = safeUnbox(issue.getRemainingEstimate());
                long increase = adjustmentSeconds != null ? adjustmentSeconds : timeSpentDelta;
                issue.setRemainingEstimate(current + increase);
            }
        }
    }

    private String resolveAuthorName(UUID authorId) {
        if (authorId == null) return null;
        try {
            RestTemplate rt = new RestTemplate();
            @SuppressWarnings("unchecked")
            Map<String, Object> user = rt.getForObject(userServiceUrl + "/api/users/" + authorId, Map.class);
            if (user != null) {
                Object name = user.get("displayName");
                if (name == null) name = user.get("username");
                if (name != null) return name.toString();
            }
        } catch (Exception e) {
            log.debug("Could not resolve author name for {}: {}", authorId, e.getMessage());
        }
        return null;
    }

    private WorklogResponse toResponse(Worklog worklog) {
        return WorklogResponse.builder()
                .id(worklog.getId())
                .issueId(worklog.getIssueId())
                .authorId(worklog.getAuthorId())
                .authorName(resolveAuthorName(worklog.getAuthorId()))
                .timeSpentSeconds(worklog.getTimeSpentSeconds())
                .workDescription(worklog.getWorkDescription())
                .startedAt(worklog.getStartedAt())
                .createdAt(worklog.getCreatedAt())
                .updatedAt(worklog.getUpdatedAt())
                .visibility(worklog.getVisibility())
                .visibilityGroupId(worklog.getVisibilityGroupId())
                .build();
    }

    private static long safeUnbox(Long value) {
        return value != null ? value : 0L;
    }

    private static String formatTimeDisplay(long seconds) {
        long s = seconds;
        long weeks = s / (5 * 8 * 3600);
        s = s % (5 * 8 * 3600);
        long days = s / (8 * 3600);
        s = s % (8 * 3600);
        long hours = s / 3600;
        s = s % 3600;
        long minutes = s / 60;
        StringBuilder sb = new StringBuilder();
        if (weeks > 0) sb.append(weeks).append("w ");
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m");
        String result = sb.toString().trim();
        return result.isEmpty() ? "0m" : result;
    }
}
