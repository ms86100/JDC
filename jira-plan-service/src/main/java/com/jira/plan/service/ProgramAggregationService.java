package com.jira.plan.service;

import com.jira.plan.dto.response.ProgramResponse;
import com.jira.plan.entity.Plan;
import com.jira.plan.entity.PlanItem;
import com.jira.plan.entity.Program;
import com.jira.plan.exception.ResourceNotFoundException;
import com.jira.plan.repository.PlanRepository;
import com.jira.plan.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgramAggregationService {

    private final ProgramRepository programRepository;
    private final PlanRepository planRepository;
    private final IssueSourceService issueSourceService;

    @Transactional(readOnly = true)
    public ProgramAggregationResponse getProgramAggregation(UUID programId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId));

        Set<Plan> plans = program.getPlans();
        List<AggregatedPlan> aggregatedPlans = new ArrayList<>();
        AggregatedMetrics totalMetrics = AggregatedMetrics.builder()
                .totalIssues(0)
                .totalStoryPoints(0)
                .completionPercentage(0.0)
                .build();

        for (Plan plan : plans) {
            List<PlanItem> issues = issueSourceService.getAggregatedIssues(plan.getId());
            AggregatedPlan planData = buildAggregatedPlan(plan, issues);
            aggregatedPlans.add(planData);
            aggregateMetrics(totalMetrics, planData.metrics);
        }

        List<CrossPlanDependency> crossPlanDeps = findCrossPlanDependencies(aggregatedPlans);
        List<AggregatedRelease> releases = aggregateReleases(aggregatedPlans);

        return ProgramAggregationResponse.builder()
                .programId(programId)
                .programName(program.getName())
                .planCount(plans.size())
                .plans(aggregatedPlans)
                .totalMetrics(totalMetrics)
                .crossPlanDependencies(crossPlanDeps)
                .releases(releases)
                .build();
    }

    private AggregatedPlan buildAggregatedPlan(Plan plan, List<PlanItem> issues) {
        AggregatedMetrics metrics = AggregatedMetrics.builder()
                .totalIssues(0)
                .totalStoryPoints(0)
                .completionPercentage(0.0)
                .build();

        Map<String, List<PlanItem>> issuesByType = new HashMap<>();
        for (PlanItem issue : issues) {
            issuesByType.computeIfAbsent(issue.getIssueType(), k -> new ArrayList<>()).add(issue);
            metrics.totalIssues++;
            if (issue.getStoryPoints() != null) {
                metrics.totalStoryPoints += issue.getStoryPoints();
            }
        }

        metrics.epicCount = issuesByType.getOrDefault("EPIC", List.of()).size();
        metrics.storyCount = issuesByType.getOrDefault("STORY", List.of()).size();
        metrics.taskCount = issuesByType.getOrDefault("TASK", List.of()).size();
        metrics.subtaskCount = issuesByType.getOrDefault("SUBTASK", List.of()).size();

        long completedCount = issues.stream()
                .filter(i -> "DONE".equalsIgnoreCase(i.getStatusCategory()))
                .count();
        metrics.completionPercentage = metrics.totalIssues > 0
                ? (completedCount * 100.0) / metrics.totalIssues
                : 0;

        Map<String, Object> planSettings = plan.getSettings();
        DateRange dateRange = calculateDateRange(issues);
        metrics.startDate = dateRange.start;
        metrics.endDate = dateRange.end;

        return AggregatedPlan.builder()
                .planId(plan.getId())
                .planName(plan.getName())
                .issuesByType(issuesByType)
                .metrics(metrics)
                .build();
    }

    private DateRange calculateDateRange(List<PlanItem> issues) {
        DateRange range = new DateRange();
        for (PlanItem issue : issues) {
            if (issue.getTargetDate() != null) {
                if (range.start == null || issue.getTargetDate().isBefore(range.start)) {
                    range.start = issue.getTargetDate();
                }
                if (range.end == null || issue.getTargetDate().isAfter(range.end)) {
                    range.end = issue.getTargetDate();
                }
            }
        }
        return range;
    }

    private void aggregateMetrics(AggregatedMetrics total, AggregatedMetrics planMetrics) {
        total.totalIssues += planMetrics.totalIssues;
        total.epicCount += planMetrics.epicCount;
        total.storyCount += planMetrics.storyCount;
        total.taskCount += planMetrics.taskCount;
        total.subtaskCount += planMetrics.subtaskCount;
        total.totalStoryPoints += planMetrics.totalStoryPoints;
        total.completionPercentage =
                (total.completionPercentage + planMetrics.completionPercentage) / 2;
        if (planMetrics.startDate != null) {
            if (total.startDate == null || planMetrics.startDate.isBefore(total.startDate)) {
                total.startDate = planMetrics.startDate;
            }
        }
        if (planMetrics.endDate != null) {
            if (total.endDate == null || planMetrics.endDate.isAfter(total.endDate)) {
                total.endDate = planMetrics.endDate;
            }
        }
    }

    private List<CrossPlanDependency> findCrossPlanDependencies(List<AggregatedPlan> plans) {
        List<CrossPlanDependency> dependencies = new ArrayList<>();
        Map<String, AggregatedPlan> planIssueMap = new HashMap<>();

        for (AggregatedPlan plan : plans) {
            for (PlanItem issue : plan.getAllIssues()) {
                String key = plan.getPlanId() + ":" + issue.getIssueKey();
                planIssueMap.put(key, plan);
            }
        }

        for (AggregatedPlan plan : plans) {
            for (PlanItem issue : plan.getAllIssues()) {
                // Look for blocking relationships within the plan's issues
                // that reference issues from other plans
            }
        }

        return dependencies;
    }

    private List<AggregatedRelease> aggregateReleases(List<AggregatedPlan> plans) {
        Map<String, AggregatedRelease> releaseMap = new LinkedHashMap<>();

        for (AggregatedPlan plan : plans) {
            for (PlanItem issue : plan.getAllIssues()) {
                // Extract version/release info from issue if available
                // Group by release name
            }
        }

        return new ArrayList<>(releaseMap.values());
    }

    @lombok.Data
    @lombok.Builder
    public static class AggregatedPlan {
        private UUID planId;
        private String planName;
        private Map<String, List<PlanItem>> issuesByType;
        private AggregatedMetrics metrics;

        public List<PlanItem> getAllIssues() {
            return issuesByType.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class AggregatedMetrics {
        private int totalIssues;
        private int epicCount;
        private int storyCount;
        private int taskCount;
        private int subtaskCount;
        private int totalStoryPoints;
        private double completionPercentage;
        private java.time.LocalDate startDate;
        private java.time.LocalDate endDate;
    }

    @lombok.Data
    @lombok.Builder
    public static class CrossPlanDependency {
        private String fromPlanId;
        private String fromIssueKey;
        private String toPlanId;
        private String toIssueKey;
        private String dependencyType;
    }

    @lombok.Data
    @lombok.Builder
    public static class AggregatedRelease {
        private String name;
        private java.time.LocalDate releaseDate;
        private int issueCount;
        private double progress;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class DateRange {
        public java.time.LocalDate start;
        public java.time.LocalDate end;
    }

    @lombok.Data
    @lombok.Builder
    public static class ProgramAggregationResponse {
        private UUID programId;
        private String programName;
        private int planCount;
        private List<AggregatedPlan> plans;
        private AggregatedMetrics totalMetrics;
        private List<CrossPlanDependency> crossPlanDependencies;
        private List<AggregatedRelease> releases;
    }
}