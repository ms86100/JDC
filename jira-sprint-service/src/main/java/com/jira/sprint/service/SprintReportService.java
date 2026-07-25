package com.jira.sprint.service;

import com.jira.cluster.util.StatusCategoryHelper;
import com.jira.sprint.dto.*;
import com.jira.sprint.entity.Sprint;
import com.jira.sprint.entity.SprintIssue;
import com.jira.sprint.exception.ResourceNotFoundException;
import com.jira.sprint.repository.SprintIssueRepository;
import com.jira.sprint.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SprintReportService {

    private final SprintRepository sprintRepository;
    private final SprintIssueRepository sprintIssueRepository;
    private final IssueServiceClient issueServiceClient;
    private final MessageSource messageSource;

    @Value("${app.defaults.status-fallback:To Do}")
    private String defaultStatusFallback;

    @Value("${app.report.priority-categories:Highest,High,Medium,Low,Lowest}")
    private String priorityCategoriesStr;

    @Value("${app.report.type-categories:Bug,Story,Task,Epic,Other}")
    private String typeCategoriesStr;

    @Value("${app.report.default-priority-fallback:Medium}")
    private String defaultPriorityFallback;

    @Transactional(readOnly = true)
    public SprintReportResponse getSprintReport(UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSource.getMessage("error.sprint.not.found", new Object[]{sprintId}, java.util.Locale.ENGLISH)));

        List<SprintIssue> sprintIssues = sprintIssueRepository.findBySprintId(sprintId);

        // Calculate basic metrics
        int totalIssues = sprintIssues.size();
        int completedIssues = 0;
        int inProgressIssues = 0;
        int todoIssues = 0;

        // Get real story points from issue service
        int totalPoints = 0;
        int completedPoints = 0;
        Map<String, Integer> pointsByStatus = calculateIssuePointsByStatus(sprintIssues);

        for (SprintIssue issue : sprintIssues) {
            String status = getIssueStatus(issue.getIssueId());
            if (StatusCategoryHelper.isCompleted(status)) {
                completedIssues++;
            } else if (StatusCategoryHelper.isInProgress(status)) {
                inProgressIssues++;
            } else {
                todoIssues++;
            }
        }

        totalPoints = pointsByStatus.values().stream().mapToInt(Integer::intValue).sum();
        completedPoints = pointsByStatus.getOrDefault("DONE", 0);

        // Calculate remaining points
        int remainingPoints = totalPoints - completedPoints;
        double completionRate = totalIssues > 0 ? (double) completedIssues / totalIssues * 100 : 0;
        double pointsCompletionRate = totalPoints > 0 ? (double) completedPoints / totalPoints * 100 : 0;

        // Time metrics
        LocalDate today = LocalDate.now();
        int daysRemaining = 0;
        if (sprint.getEndDate() != null) {
            daysRemaining = (int) ChronoUnit.DAYS.between(today, sprint.getEndDate());
            if (daysRemaining < 0) daysRemaining = 0;
        }

        double dailyBurnRate = 0;
        if (sprint.getStartDate() != null && completedPoints > 0) {
            long daysElapsed = Math.max(1, ChronoUnit.DAYS.between(sprint.getStartDate(), today));
            dailyBurnRate = (double) completedPoints / daysElapsed;
        }

        int projectedCompletion = 0;
        if (dailyBurnRate > 0) {
            projectedCompletion = (int) (completedPoints + (dailyBurnRate * daysRemaining));
        }

        // Work distribution
        Map<String, Integer> issuesByStatus = new HashMap<>();
        issuesByStatus.put("TODO", todoIssues);
        issuesByStatus.put("IN_PROGRESS", inProgressIssues);
        issuesByStatus.put("DONE", completedIssues);

        // Get real priority distribution from issue service
        Map<String, Integer> issuesByPriority = calculateIssueCountByPriority(sprintIssues);

        // Get real type distribution from issue service
        Map<String, Integer> issuesByType = calculateIssueCountByType(sprintIssues);

        // Get real assignee distribution from issue service
        Map<String, Integer> issuesByAssignee = calculateIssueCountByAssignee(sprintIssues);

        // Generate burndown data
        BurndownResponse burndown = generateBurndownData(sprint, sprintIssues, totalPoints, completedPoints);

        // Generate velocity data
        VelocityResponse velocity = generateVelocityData(sprint.getProjectId());

        return SprintReportResponse.builder()
                .sprintId(sprint.getId())
                .sprintName(sprint.getName())
                .sprintGoal(sprint.getGoal())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .completeDate(sprint.getCompleteDate())
                .status(sprint.getStatus().name())
                .totalIssues(totalIssues)
                .completedIssues(completedIssues)
                .inProgressIssues(inProgressIssues)
                .todoIssues(todoIssues)
                .blockedIssues(0)
                .totalPoints(totalPoints)
                .completedPoints(completedPoints)
                .remainingPoints(remainingPoints)
                .completionRate(Math.round(completionRate * 100.0) / 100.0)
                .pointsCompletionRate(Math.round(pointsCompletionRate * 100.0) / 100.0)
                .daysRemaining(daysRemaining)
                .dailyBurnRate(Math.round(dailyBurnRate * 100.0) / 100.0)
                .projectedCompletion(projectedCompletion)
                .issuesByStatus(issuesByStatus)
                .issuesByPriority(issuesByPriority)
                .issuesByType(issuesByType)
                .issuesByAssignee(issuesByAssignee)
                .burndown(burndown)
                .velocity(velocity)
                .issuesAddedDuringSprint(getScopeAddedIssueKeys(sprint))
                .issuesRemovedDuringSprint(getScopeRemovedIssueKeys(sprintId))
                .issuesNotCompleted(getNotCompletedIssueKeys(sprintIssues))
                .build();
    }

    @Transactional(readOnly = true)
    public BurndownResponse getBurndown(UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException(messageSource.getMessage("error.sprint.not.found", new Object[]{sprintId}, java.util.Locale.ENGLISH)));

        List<SprintIssue> sprintIssues = sprintIssueRepository.findBySprintId(sprintId);

        // Get real story points from issue service
        Map<String, Integer> pointsByStatus = calculateIssuePointsByStatus(sprintIssues);
        int totalPoints = pointsByStatus.values().stream().mapToInt(Integer::intValue).sum();
        int completedPoints = pointsByStatus.getOrDefault("DONE", 0);

        return generateBurndownData(sprint, sprintIssues, totalPoints, completedPoints);
    }

    @Transactional(readOnly = true)
    public VelocityResponse getVelocity(UUID projectId) {
        return generateVelocityData(projectId);
    }

    private BurndownResponse generateBurndownData(Sprint sprint, List<SprintIssue> sprintIssues,
                                                   int totalPoints, int completedPoints) {
        return generateBurndownData(sprint, sprintIssues, totalPoints, completedPoints, null, null);
    }

    private BurndownResponse generateBurndownData(Sprint sprint, List<SprintIssue> sprintIssues,
                                                   int totalPoints, int completedPoints,
                                                   String workingDays, String nonWorkingDates) {
        List<BurndownDataPoint> dailyData = new ArrayList<>();

        LocalDate startDate = sprint.getStartDate() != null ? sprint.getStartDate() : LocalDate.now().minusDays(7);
        LocalDate endDate = sprint.getEndDate() != null ? sprint.getEndDate() : LocalDate.now().plusDays(7);
        LocalDate today = LocalDate.now();

        Set<java.time.DayOfWeek> workDays = parseWorkingDays(workingDays);
        Set<LocalDate> holidays = parseNonWorkingDates(nonWorkingDates);

        long totalWorkingDays = countWorkingDays(startDate, endDate, workDays, holidays);
        if (totalWorkingDays <= 0) totalWorkingDays = 10;
        double idealBurnPerWorkDay = totalPoints / (double) totalWorkingDays;

        int remainingPoints = totalPoints;
        LocalDate currentDate = startDate;
        int workDayIndex = 0;

        while (!currentDate.isAfter(endDate)) {
            boolean isWorkDay = workDays.contains(currentDate.getDayOfWeek()) && !holidays.contains(currentDate);

            double idealPoints;
            if (isWorkDay) {
                idealPoints = Math.max(0, totalPoints - (idealBurnPerWorkDay * workDayIndex));
                workDayIndex++;
            } else {
                idealPoints = workDayIndex > 0
                        ? Math.max(0, totalPoints - (idealBurnPerWorkDay * (workDayIndex)))
                        : totalPoints;
            }

            if (currentDate.isAfter(today)) {
                remainingPoints = (int) Math.max(0, idealPoints);
            }

            BurndownDataPoint point = BurndownDataPoint.builder()
                    .date(currentDate)
                    .remainingPoints((double) remainingPoints)
                    .idealPoints(Math.round(idealPoints * 100.0) / 100.0)
                    .totalIssues(sprintIssues.size())
                    .completedIssues(currentDate.isAfter(startDate) ?
                            (int) (totalPoints - remainingPoints) : 0)
                    .addedIssues(currentDate.equals(startDate) ? sprintIssues.size() : 0)
                    .removedIssues(0)
                    .build();

            dailyData.add(point);
            currentDate = currentDate.plusDays(1);
        }

        return BurndownResponse.builder()
                .sprintId(sprint.getId())
                .sprintName(sprint.getName())
                .startDate(startDate)
                .endDate(endDate)
                .totalPoints(totalPoints)
                .completedPoints(completedPoints)
                .remainingPoints(remainingPoints)
                .totalIssues(sprintIssues.size())
                .completedIssues(completedPoints / 3)
                .completionRate(totalPoints > 0 ? Math.round((double) completedPoints / totalPoints * 10000.0) / 100.0 : 0)
                .dailyData(dailyData)
                .build();
    }

    private VelocityResponse generateVelocityData(UUID projectId) {
        List<Sprint> projectSprints = sprintRepository.findByProjectIdOrderByCreatedAtDesc(projectId);

        List<SprintVelocity> sprintVelocities = new ArrayList<>();
        List<Integer> completedPointsList = new ArrayList<>();

        for (Sprint sprint : projectSprints) {
            List<SprintIssue> issues = sprintIssueRepository.findBySprintId(sprint.getId());

            // Get real story points from issue service
            int committed = 0;
            int completed = 0;
            for (SprintIssue si : issues) {
                int points = getIssueStoryPoints(si.getIssueId());
                committed += points;
                if (StatusCategoryHelper.isCompleted(getIssueStatus(si.getIssueId()))) {
                    completed += points;
                }
            }

            if (sprint.getStatus() == Sprint.SprintStatus.CLOSED) {
                completedPointsList.add(completed);
            }

            double reliability = committed > 0 ? (double) completed / committed * 100 : 0;

            sprintVelocities.add(SprintVelocity.builder()
                    .sprintId(sprint.getId())
                    .sprintName(sprint.getName())
                    .startDate(sprint.getStartDate())
                    .endDate(sprint.getEndDate())
                    .committedPoints(committed)
                    .completedPoints(completed)
                    .reliability(Math.round(reliability * 100.0) / 100.0)
                    .isCompleted(sprint.getStatus() == Sprint.SprintStatus.CLOSED)
                    .build());
        }

        // Calculate velocity metrics
        int completedSprints = (int) completedPointsList.stream().count();
        double avgVelocity = completedPointsList.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);
        int maxVelocity = completedPointsList.stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        int minVelocity = completedPointsList.stream()
                .mapToInt(Integer::intValue)
                .min()
                .orElse(0);

        double trend = 0;
        if (completedPointsList.size() >= 2) {
            int recent = completedPointsList.get(0);
            int previous = completedPointsList.get(1);
            trend = previous > 0 ? ((double) (recent - previous) / previous) * 100 : 0;
        }

        return VelocityResponse.builder()
                .projectId(projectId)
                .currentVelocity(completedPointsList.isEmpty() ? 0 : completedPointsList.get(0))
                .averageVelocity(Math.round(avgVelocity * 100.0) / 100.0)
                .highestVelocity(maxVelocity)
                .lowestVelocity(minVelocity)
                .totalSprints(projectSprints.size())
                .completedSprints(completedSprints)
                .velocityTrend(Math.round(trend * 100.0) / 100.0)
                .sprintVelocities(sprintVelocities)
                .build();
    }

    private String getIssueStatus(UUID issueId) {
        try {
            IssueServiceClient.IssueData issue = issueServiceClient.getIssue(issueId);
            return issue.getStatusName() != null ? issue.getStatusName() : defaultStatusFallback;
        } catch (Exception e) {
            log.warn("Failed to get issue status for {}: {}", issueId, e.getMessage());
            return defaultStatusFallback;
        }
    }

    private int getIssueStoryPoints(UUID issueId) {
        return getIssueEstimation(issueId, "STORY_POINTS");
    }

    private int getIssueEstimation(UUID issueId, String estimationStatistic) {
        try {
            IssueServiceClient.IssueData issue = issueServiceClient.getIssue(issueId);
            if (estimationStatistic == null) estimationStatistic = "STORY_POINTS";
            return switch (estimationStatistic.toUpperCase()) {
                case "STORY_POINTS" -> issue.getStoryPoints() != null ? issue.getStoryPoints() : 0;
                case "BUSINESS_VALUE" -> issue.getBusinessValue() != null ? issue.getBusinessValue() : 0;
                case "ORIGINAL_TIME_ESTIMATE" -> issue.getOriginalEstimate() != null ? issue.getOriginalEstimate().intValue() : 0;
                case "ISSUE_COUNT" -> 1;
                default -> issue.getStoryPoints() != null ? issue.getStoryPoints() : 0;
            };
        } catch (Exception e) {
            log.warn("Failed to get estimation for {}: {}", issueId, e.getMessage());
            return 0;
        }
    }

    private Map<String, Integer> calculateIssuePointsByStatus(List<SprintIssue> sprintIssues) {
        Map<String, Integer> pointsByStatus = new HashMap<>();
        pointsByStatus.put("TODO", 0);
        pointsByStatus.put("IN_PROGRESS", 0);
        pointsByStatus.put("DONE", 0);

        for (SprintIssue si : sprintIssues) {
            int points = getIssueStoryPoints(si.getIssueId());
            String status = getIssueStatus(si.getIssueId());

            String category = StatusCategoryHelper.getCategory(status);
            pointsByStatus.merge(category, points, Integer::sum);
        }
        return pointsByStatus;
    }

    /**
     * Calculate real issue count by priority using actual data from issue service.
     */
    private Map<String, Integer> calculateIssueCountByPriority(List<SprintIssue> sprintIssues) {
        Map<String, Integer> priorityCounts = new LinkedHashMap<>();
        for (String p : priorityCategoriesStr.split(",")) {
            priorityCounts.put(p.trim(), 0);
        }

        for (SprintIssue si : sprintIssues) {
            try {
                IssueServiceClient.IssueData issue = issueServiceClient.getIssue(si.getIssueId());
                String priority = issue.getPriorityName();
                if (priority != null && priorityCounts.containsKey(priority)) {
                    priorityCounts.merge(priority, 1, Integer::sum);
                } else if (priority != null) {
                    priorityCounts.merge(defaultPriorityFallback, 1, Integer::sum);
                }
            } catch (Exception e) {
                log.debug("Failed to get priority for issue {}: {}", si.getIssueId(), e.getMessage());
            }
        }

        return priorityCounts;
    }

    /**
     * Calculate real issue count by type using actual data from issue service.
     */
    private Map<String, Integer> calculateIssueCountByType(List<SprintIssue> sprintIssues) {
        Map<String, Integer> typeCounts = new LinkedHashMap<>();
        String[] types = typeCategoriesStr.split(",");
        for (String t : types) {
            typeCounts.put(t.trim(), 0);
        }
        String fallbackType = types[types.length - 1].trim();

        for (SprintIssue si : sprintIssues) {
            try {
                IssueServiceClient.IssueData issue = issueServiceClient.getIssue(si.getIssueId());
                String type = issue.getIssueTypeName();
                if (type != null && typeCounts.containsKey(type)) {
                    typeCounts.merge(type, 1, Integer::sum);
                } else if (type != null) {
                    typeCounts.merge(fallbackType, 1, Integer::sum);
                }
            } catch (Exception e) {
                log.debug("Failed to get type for issue {}: {}", si.getIssueId(), e.getMessage());
            }
        }

        return typeCounts;
    }

    /**
     * Calculate real issue count by assignee using actual data from issue service.
     */
    private Map<String, Integer> calculateIssueCountByAssignee(List<SprintIssue> sprintIssues) {
        Map<String, Integer> assigneeCounts = new LinkedHashMap<>();
        assigneeCounts.put("Unassigned", 0);

        for (SprintIssue si : sprintIssues) {
            try {
                IssueServiceClient.IssueData issue = issueServiceClient.getIssue(si.getIssueId());
                String assignee = issue.getAssigneeName();
                if (assignee != null && !assignee.isEmpty()) {
                    assigneeCounts.merge(assignee, 1, Integer::sum);
                } else {
                    assigneeCounts.merge("Unassigned", 1, Integer::sum);
                }
            } catch (Exception e) {
                log.debug("Failed to get assignee for issue {}: {}", si.getIssueId(), e.getMessage());
                assigneeCounts.merge("Unassigned", 1, Integer::sum);
            }
        }

        return assigneeCounts;
    }

    private Set<java.time.DayOfWeek> parseWorkingDays(String workingDays) {
        Set<java.time.DayOfWeek> days = new LinkedHashSet<>();
        if (workingDays == null || workingDays.isBlank()) {
            days.add(java.time.DayOfWeek.MONDAY);
            days.add(java.time.DayOfWeek.TUESDAY);
            days.add(java.time.DayOfWeek.WEDNESDAY);
            days.add(java.time.DayOfWeek.THURSDAY);
            days.add(java.time.DayOfWeek.FRIDAY);
            return days;
        }
        for (String d : workingDays.split(",")) {
            try {
                days.add(java.time.DayOfWeek.valueOf(d.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                try {
                    days.add(java.time.DayOfWeek.valueOf(expandDayAbbrev(d.trim())));
                } catch (IllegalArgumentException ignored2) {}
            }
        }
        return days.isEmpty() ? Set.of(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY,
                java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY) : days;
    }

    private String expandDayAbbrev(String abbrev) {
        return switch (abbrev.toUpperCase()) {
            case "MON" -> "MONDAY";
            case "TUE" -> "TUESDAY";
            case "WED" -> "WEDNESDAY";
            case "THU" -> "THURSDAY";
            case "FRI" -> "FRIDAY";
            case "SAT" -> "SATURDAY";
            case "SUN" -> "SUNDAY";
            default -> abbrev;
        };
    }

    private Set<LocalDate> parseNonWorkingDates(String nonWorkingDates) {
        Set<LocalDate> dates = new HashSet<>();
        if (nonWorkingDates == null || nonWorkingDates.isBlank()) return dates;
        try {
            String clean = nonWorkingDates.replaceAll("[\\[\\]\"]", "");
            for (String d : clean.split(",")) {
                if (!d.trim().isEmpty()) {
                    dates.add(LocalDate.parse(d.trim()));
                }
            }
        } catch (Exception e) {
            log.debug("Could not parse non-working dates: {}", e.getMessage());
        }
        return dates;
    }

    private long countWorkingDays(LocalDate start, LocalDate end, Set<java.time.DayOfWeek> workDays, Set<LocalDate> holidays) {
        long count = 0;
        LocalDate d = start;
        while (!d.isAfter(end)) {
            if (workDays.contains(d.getDayOfWeek()) && !holidays.contains(d)) {
                count++;
            }
            d = d.plusDays(1);
        }
        return count;
    }

    private List<String> getScopeAddedIssueKeys(Sprint sprint) {
        if (sprint.getStartDate() == null) return List.of();
        java.time.LocalDateTime sprintStart = sprint.getStartDate().atStartOfDay();
        List<SprintIssue> added = sprintIssueRepository.findBySprintIdAndAddedAtAfter(sprint.getId(), sprintStart);
        return added.stream()
                .map(si -> {
                    try {
                        return issueServiceClient.getIssue(si.getIssueId()).getIssueKey();
                    } catch (Exception e) { return si.getIssueId().toString(); }
                })
                .collect(Collectors.toList());
    }

    private List<String> getScopeRemovedIssueKeys(UUID sprintId) {
        List<SprintIssue> removed = sprintIssueRepository.findBySprintIdAndRemovedAtIsNotNull(sprintId);
        return removed.stream()
                .map(si -> {
                    try {
                        return issueServiceClient.getIssue(si.getIssueId()).getIssueKey();
                    } catch (Exception e) { return si.getIssueId().toString(); }
                })
                .collect(Collectors.toList());
    }

    private List<String> getNotCompletedIssueKeys(List<SprintIssue> sprintIssues) {
        return sprintIssues.stream()
                .filter(si -> si.getRemovedAt() == null)
                .filter(si -> {
                    String status = getIssueStatus(si.getIssueId());
                    return !StatusCategoryHelper.isCompleted(status);
                })
                .map(si -> {
                    try {
                        return issueServiceClient.getIssue(si.getIssueId()).getIssueKey();
                    } catch (Exception e) { return si.getIssueId().toString(); }
                })
                .collect(Collectors.toList());
    }
}