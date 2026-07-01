package com.jira.sprint.service;

import com.jira.sprint.dto.*;
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

    @Transactional(readOnly = true)
    public SprintReportResponse getSprintReport(UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found: " + sprintId));

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
            if ("Done".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
                completedIssues++;
            } else if ("In Progress".equalsIgnoreCase(status)) {
                inProgressIssues++;
            } else {
                todoIssues++;
            }
        }

        totalPoints = pointsByStatus.values().stream().mapToInt(Integer::intValue).sum();
        completedPoints = pointsByStatus.getOrDefault("Done", 0);

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
        issuesByStatus.put("To Do", todoIssues);
        issuesByStatus.put("In Progress", inProgressIssues);
        issuesByStatus.put("Done", completedIssues);

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
                .build();
    }

    @Transactional(readOnly = true)
    public BurndownResponse getBurndown(UUID sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found: " + sprintId));

        List<SprintIssue> sprintIssues = sprintIssueRepository.findBySprintId(sprintId);

        // Get real story points from issue service
        Map<String, Integer> pointsByStatus = calculateIssuePointsByStatus(sprintIssues);
        int totalPoints = pointsByStatus.values().stream().mapToInt(Integer::intValue).sum();
        int completedPoints = pointsByStatus.getOrDefault("Done", 0);

        return generateBurndownData(sprint, sprintIssues, totalPoints, completedPoints);
    }

    @Transactional(readOnly = true)
    public VelocityResponse getVelocity(UUID projectId) {
        return generateVelocityData(projectId);
    }

    private BurndownResponse generateBurndownData(Sprint sprint, List<SprintIssue> sprintIssues,
                                                   int totalPoints, int completedPoints) {
        List<BurndownDataPoint> dailyData = new ArrayList<>();

        LocalDate startDate = sprint.getStartDate() != null ? sprint.getStartDate() : LocalDate.now().minusDays(7);
        LocalDate endDate = sprint.getEndDate() != null ? sprint.getEndDate() : LocalDate.now().plusDays(7);
        LocalDate today = LocalDate.now();

        // Calculate total days and ideal burn rate
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        if (totalDays <= 0) totalDays = 14; // Default 2 week sprint
        double idealDailyBurn = totalPoints / (double) totalDays;

        int remainingPoints = totalPoints;
        LocalDate currentDate = startDate;
        int dayIndex = 0;

        while (!currentDate.isAfter(endDate)) {
            double idealPoints = Math.max(0, totalPoints - (idealDailyBurn * dayIndex));

            // Simulate actual burndown based on current completion
            if (currentDate.isAfter(today)) {
                // Future dates - use trend projection
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
            dayIndex++;
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
                if ("Done".equalsIgnoreCase(getIssueStatus(si.getIssueId()))) {
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
            return issue.getStatusName() != null ? issue.getStatusName() : "To Do";
        } catch (Exception e) {
            log.warn("Failed to get issue status for {}: {}", issueId, e.getMessage());
            return "To Do";
        }
    }

    private int getIssueStoryPoints(UUID issueId) {
        try {
            IssueServiceClient.IssueData issue = issueServiceClient.getIssue(issueId);
            return issue.getStoryPoints() != null ? issue.getStoryPoints() : 0;
        } catch (Exception e) {
            log.warn("Failed to get story points for {}: {}", issueId, e.getMessage());
            return 0;
        }
    }

    private Map<String, Integer> calculateIssuePointsByStatus(List<SprintIssue> sprintIssues) {
        Map<String, Integer> pointsByStatus = new HashMap<>();
        pointsByStatus.put("To Do", 0);
        pointsByStatus.put("In Progress", 0);
        pointsByStatus.put("Done", 0);

        for (SprintIssue si : sprintIssues) {
            int points = getIssueStoryPoints(si.getIssueId());
            String status = getIssueStatus(si.getIssueId());

            if (status == null) {
                pointsByStatus.merge("To Do", points, Integer::sum);
            } else if (status.contains("Done") || status.contains("Completed") || status.equalsIgnoreCase("Closed")) {
                pointsByStatus.merge("Done", points, Integer::sum);
            } else if (status.contains("Progress")) {
                pointsByStatus.merge("In Progress", points, Integer::sum);
            } else {
                pointsByStatus.merge("To Do", points, Integer::sum);
            }
        }
        return pointsByStatus;
    }

    /**
     * Calculate real issue count by priority using actual data from issue service.
     */
    private Map<String, Integer> calculateIssueCountByPriority(List<SprintIssue> sprintIssues) {
        Map<String, Integer> priorityCounts = new LinkedHashMap<>();
        priorityCounts.put("Highest", 0);
        priorityCounts.put("High", 0);
        priorityCounts.put("Medium", 0);
        priorityCounts.put("Low", 0);
        priorityCounts.put("Lowest", 0);

        for (SprintIssue si : sprintIssues) {
            try {
                IssueServiceClient.IssueData issue = issueServiceClient.getIssue(si.getIssueId());
                String priority = issue.getPriorityName();
                if (priority != null && priorityCounts.containsKey(priority)) {
                    priorityCounts.merge(priority, 1, Integer::sum);
                } else if (priority != null) {
                    priorityCounts.merge("Medium", 1, Integer::sum); // Default unknown to Medium
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
        typeCounts.put("Bug", 0);
        typeCounts.put("Story", 0);
        typeCounts.put("Task", 0);
        typeCounts.put("Epic", 0);
        typeCounts.put("Other", 0);

        for (SprintIssue si : sprintIssues) {
            try {
                IssueServiceClient.IssueData issue = issueServiceClient.getIssue(si.getIssueId());
                String type = issue.getIssueTypeName();
                if (type != null && typeCounts.containsKey(type)) {
                    typeCounts.merge(type, 1, Integer::sum);
                } else if (type != null) {
                    typeCounts.merge("Other", 1, Integer::sum);
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
}