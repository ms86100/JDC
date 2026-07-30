package com.avionics_systems.plan.service;

import com.avionics_systems.cluster.util.StatusCategoryHelper;
import com.avionics_systems.plan.dto.response.ProgramResponse;
import com.avionics_systems.plan.entity.Plan;
import com.avionics_systems.plan.entity.PlanItem;
import com.avionics_systems.plan.entity.Program;
import com.avionics_systems.plan.exception.ResourceNotFoundException;
import com.avionics_systems.plan.repository.PlanRepository;
import com.avionics_systems.plan.repository.ProgramRepository;
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
                .filter(i -> StatusCategoryHelper.isCompleted(i.getStatusCategory()))
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
        Map<String, PlanItem> issueLookup = new HashMap<>();

        for (AggregatedPlan plan : plans) {
            for (PlanItem issue : plan.getAllIssues()) {
                String key = plan.getPlanId() + ":" + issue.getIssueKey();
                planIssueMap.put(key, plan);
                issueLookup.put(issue.getIssueKey(), issue);
            }
        }

        for (AggregatedPlan plan : plans) {
            for (PlanItem issue : plan.getAllIssues()) {
                if (issue.getParentId() != null) {
                    PlanItem parent = issueLookup.get(issue.getIssueKey());
                    if (parent != null) {
                        AggregatedPlan parentPlan = findPlanForIssue(planIssueMap, parent.getParentId());
                        if (parentPlan != null && !parentPlan.getPlanId().equals(plan.getPlanId())) {
                            dependencies.add(CrossPlanDependency.builder()
                                    .fromPlanId(plan.getPlanId().toString())
                                    .fromIssueKey(issue.getIssueKey())
                                    .toPlanId(parentPlan.getPlanId().toString())
                                    .toIssueKey(parent.getIssueKey())
                                    .dependencyType("PARENT")
                                    .build());
                        }
                    }
                }
            }
        }

        return dependencies;
    }

    private AggregatedPlan findPlanForIssue(Map<String, AggregatedPlan> planIssueMap, UUID issueId) {
        for (Map.Entry<String, AggregatedPlan> entry : planIssueMap.entrySet()) {
            if (entry.getKey().contains(issueId.toString())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private List<AggregatedRelease> aggregateReleases(List<AggregatedPlan> plans) {
        Map<String, AggregatedRelease> releaseMap = new LinkedHashMap<>();

        for (AggregatedPlan plan : plans) {
            for (PlanItem issue : plan.getAllIssues()) {
                if (issue.getTargetDate() != null) {
                    String releaseName = "Q" + ((issue.getTargetDate().getMonthValue() - 1) / 3 + 1)
                            + " " + issue.getTargetDate().getYear();

                    releaseMap.computeIfAbsent(releaseName, name -> AggregatedRelease.builder()
                            .name(name)
                            .releaseDate(issue.getTargetDate())
                            .issueCount(0)
                            .progress(0.0)
                            .build());

                    AggregatedRelease release = releaseMap.get(releaseName);
                    release.setIssueCount(release.getIssueCount() + 1);

                    if (StatusCategoryHelper.isCompleted(issue.getStatusCategory())) {
                        release.setProgress(release.getProgress() + 1);
                    }
                }
            }
        }

        for (AggregatedRelease release : releaseMap.values()) {
            if (release.getIssueCount() > 0) {
                release.setProgress((release.getProgress() * 100.0) / release.getIssueCount());
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