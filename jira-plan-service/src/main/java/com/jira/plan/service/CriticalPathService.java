package com.jira.plan.service;

import com.jira.plan.entity.IssueDependency;
import com.jira.plan.entity.PlanItem;
import com.jira.plan.repository.IssueDependencyRepository;
import com.jira.plan.repository.PlanItemRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CriticalPathService {

    private final PlanItemRepository planItemRepository;
    private final IssueDependencyRepository dependencyRepository;
    private final WorkingDaysService workingDaysService;

    public CriticalPathResult calculateCriticalPath(UUID planId) {
        log.info("Calculating critical path for plan: {}", planId);

        List<PlanItem> items = planItemRepository.findByPlanIdAndIsActiveTrue(planId);
        List<IssueDependency> dependencies = dependencyRepository.findByPlanId(planId);

        if (items.isEmpty()) {
            return CriticalPathResult.builder()
                    .success(true)
                    .message("No items to analyze")
                    .criticalPath(List.of())
                    .build();
        }

        Map<UUID, PlanItem> itemMap = items.stream()
                .collect(Collectors.toMap(PlanItem::getId, item -> item));

        Map<UUID, List<UUID>> adjacencyList = buildAdjacencyList(dependencies);
        Map<UUID, List<UUID>> reverseAdjacency = buildReverseAdjacency(dependencies);

        Map<UUID, Integer> durations = calculateDurations(items);
        Map<UUID, Long> earliestStart = calculateEarliestStart(adjacencyList, durations, findStartNodes(dependencies));
        Map<UUID, Long> earliestFinish = calculateEarliestFinish(earliestStart, durations);
        Map<UUID, Long> latestFinish = calculateLatestFinish(reverseAdjacency, earliestFinish, findEndNodes(dependencies));
        Map<UUID, Long> latestStart = calculateLatestStart(latestFinish, durations);
        Map<UUID, Long> floatTime = calculateFloat(earliestStart, latestStart);

        List<UUID> criticalPathNodes = findCriticalPathNodes(floatTime, earliestStart, latestStart);
        List<CriticalPathNode> criticalPath = buildCriticalPathNodes(criticalPathNodes, itemMap, durations, earliestStart, latestStart, floatTime);

        long projectDuration = earliestFinish.values().stream().mapToLong(v -> v).max().orElse(0);

        return CriticalPathResult.builder()
                .success(true)
                .criticalPath(criticalPath)
                .projectDurationDays(projectDuration)
                .nodeCount(items.size())
                .criticalNodeCount(criticalPathNodes.size())
                .floatAnalysis(buildFloatAnalysis(floatTime, itemMap))
                .build();
    }

    public RiskAnalysis analyzeRisks(UUID planId, UUID changeItemId, int changeDays) {
        log.info("Analyzing risk impact for item {} with {} day change", changeItemId, changeDays);

        CriticalPathResult cpm = calculateCriticalPath(planId);
        if (!cpm.isSuccess()) {
            return RiskAnalysis.builder()
                    .success(false)
                    .message("Could not analyze risks: " + cpm.getMessage())
                    .build();
        }

        List<UUID> criticalPathIds = cpm.getCriticalPath().stream()
                .map(CriticalPathNode::getIssueId)
                .collect(Collectors.toList());

        boolean affectsCriticalPath = criticalPathIds.contains(changeItemId);

        long originalDuration = cpm.getProjectDurationDays();
        long newDuration = originalDuration;

        if (affectsCriticalPath) {
            newDuration = originalDuration + changeDays;
        }

        double riskScore = calculateRiskScore(affectsCriticalPath, changeDays, originalDuration);

        List<UUID> affectedItems = new ArrayList<>();
        if (affectsCriticalPath) {
            int changeIndex = criticalPathIds.indexOf(changeItemId);
            for (int i = changeIndex + 1; i < criticalPathIds.size(); i++) {
                affectedItems.add(criticalPathIds.get(i));
            }
        }

        String riskLevel = riskScore > 0.7 ? "HIGH" : riskScore > 0.3 ? "MEDIUM" : "LOW";

        List<String> recommendations = new ArrayList<>();
        if (affectsCriticalPath && riskScore > 0.5) {
            recommendations.add("Consider adding buffer time to downstream critical path items");
            recommendations.add("Evaluate whether scope reduction is possible");
        }
        if (changeDays > 5) {
            recommendations.add("Large delay detected - notify stakeholders");
        }
        if (!affectsCriticalPath) {
            recommendations.add("Change does not affect critical path - impact is contained");
        }

        return RiskAnalysis.builder()
                .success(true)
                .affectsCriticalPath(affectsCriticalPath)
                .originalProjectDuration(originalDuration)
                .newProjectDuration(newDuration)
                .delayDays(affectsCriticalPath ? changeDays : 0)
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .affectedItemIds(affectedItems)
                .recommendations(recommendations)
                .build();
    }

    private Map<UUID, List<UUID>> buildAdjacencyList(List<IssueDependency> dependencies) {
        Map<UUID, List<UUID>> adj = new HashMap<>();
        for (IssueDependency dep : dependencies) {
            adj.computeIfAbsent(dep.getBlockingIssueId(), k -> new ArrayList<>())
                    .add(dep.getBlockedIssueId());
        }
        return adj;
    }

    private Map<UUID, List<UUID>> buildReverseAdjacency(List<IssueDependency> dependencies) {
        Map<UUID, List<UUID>> rev = new HashMap<>();
        for (IssueDependency dep : dependencies) {
            rev.computeIfAbsent(dep.getBlockedIssueId(), k -> new ArrayList<>())
                    .add(dep.getBlockingIssueId());
        }
        return rev;
    }

    private Set<UUID> findStartNodes(List<IssueDependency> dependencies) {
        Set<UUID> blockers = dependencies.stream()
                .map(IssueDependency::getBlockingIssueId)
                .collect(Collectors.toSet());
        return dependencies.stream()
                .map(IssueDependency::getBlockedIssueId)
                .filter(id -> !blockers.contains(id))
                .collect(Collectors.toSet());
    }

    private Set<UUID> findEndNodes(List<IssueDependency> dependencies) {
        Set<UUID> blocked = dependencies.stream()
                .map(IssueDependency::getBlockedIssueId)
                .collect(Collectors.toSet());
        return dependencies.stream()
                .map(IssueDependency::getBlockingIssueId)
                .filter(id -> !blocked.contains(id))
                .collect(Collectors.toSet());
    }

    private Map<UUID, Integer> calculateDurations(List<PlanItem> items) {
        Map<UUID, Integer> durations = new HashMap<>();
        for (PlanItem item : items) {
            durations.put(item.getId(), getDurationDays(item));
        }
        return durations;
    }

    private int getDurationDays(PlanItem item) {
        Integer storyPoints = item.getStoryPoints();
        if (storyPoints == null) {
            return 5;
        }
        if (storyPoints <= 3) return 2;
        if (storyPoints <= 8) return 5;
        if (storyPoints <= 13) return 10;
        if (storyPoints <= 21) return 15;
        return 20;
    }

    private Map<UUID, Long> calculateEarliestStart(Map<UUID, List<UUID>> adj, Map<UUID, Integer> durations, Set<UUID> startNodes) {
        Map<UUID, Long> es = new HashMap<>();
        Queue<UUID> queue = new LinkedList<>(startNodes);

        while (!queue.isEmpty()) {
            UUID node = queue.poll();
            if (!es.containsKey(node)) {
                es.put(node, 0L);
            }

            List<UUID> neighbors = adj.get(node);
            if (neighbors != null) {
                for (UUID neighbor : neighbors) {
                    long newEs = Math.max(
                            es.getOrDefault(node, 0L) + durations.getOrDefault(node, 0),
                            es.getOrDefault(neighbor, 0L)
                    );
                    es.put(neighbor, newEs);
                    queue.offer(neighbor);
                }
            }
        }

        return es;
    }

    private Map<UUID, Long> calculateEarliestFinish(Map<UUID, Long> earliestStart, Map<UUID, Integer> durations) {
        Map<UUID, Long> ef = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : earliestStart.entrySet()) {
            ef.put(entry.getKey(), entry.getValue() + durations.getOrDefault(entry.getKey(), 0));
        }
        return ef;
    }

    private Map<UUID, Long> calculateLatestFinish(Map<UUID, List<UUID>> reverseAdj, Map<UUID, Long> earliestFinish, Set<UUID> endNodes) {
        long maxEf = earliestFinish.values().stream().mapToLong(v -> v).max().orElse(0);
        Map<UUID, Long> lf = new HashMap<>();

        for (UUID endNode : endNodes) {
            lf.put(endNode, maxEf);
        }

        Queue<UUID> queue = new LinkedList<>(endNodes);
        while (!queue.isEmpty()) {
            UUID node = queue.poll();
            List<UUID> predecessors = reverseAdj.get(node);
            if (predecessors != null) {
                for (UUID pred : predecessors) {
                    long minLf = Long.MAX_VALUE;
                    List<UUID> successors = new ArrayList<>();
                    if (reverseAdj.containsKey(pred)) {
                        for (UUID succ : reverseAdj.get(pred)) {
                            if (lf.containsKey(succ)) {
                                minLf = Math.min(minLf, lf.get(succ));
                            }
                        }
                    }
                    if (minLf != Long.MAX_VALUE) {
                        lf.put(pred, minLf);
                    } else {
                        lf.putIfAbsent(pred, maxEf);
                    }
                    queue.offer(pred);
                }
            }
        }

        return lf;
    }

    private Map<UUID, Long> calculateLatestStart(Map<UUID, Long> latestFinish, Map<UUID, Integer> durations) {
        Map<UUID, Long> ls = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : latestFinish.entrySet()) {
            ls.put(entry.getKey(), entry.getValue() - durations.getOrDefault(entry.getKey(), 0));
        }
        return ls;
    }

    private Map<UUID, Long> calculateFloat(Map<UUID, Long> earliestStart, Map<UUID, Long> latestStart) {
        Map<UUID, Long> floatMap = new HashMap<>();
        for (UUID node : latestStart.keySet()) {
            long es = earliestStart.getOrDefault(node, 0L);
            long ls = latestStart.getOrDefault(node, es);
            floatMap.put(node, ls - es);
        }
        return floatMap;
    }

    private List<UUID> findCriticalPathNodes(Map<UUID, Long> floatTime, Map<UUID, Long> earliestStart, Map<UUID, Long> latestStart) {
        List<UUID> critical = new ArrayList<>();
        for (UUID node : floatTime.keySet()) {
            if (floatTime.get(node) == 0) {
                critical.add(node);
            }
        }

        critical.sort(Comparator.comparing(node -> earliestStart.getOrDefault(node, 0L)));
        return critical;
    }

    private List<CriticalPathNode> buildCriticalPathNodes(List<UUID> nodeIds, Map<UUID, PlanItem> itemMap,
                                                          Map<UUID, Integer> durations,
                                                          Map<UUID, Long> earliestStart,
                                                          Map<UUID, Long> latestStart,
                                                          Map<UUID, Long> floatTime) {
        List<CriticalPathNode> path = new ArrayList<>();
        for (UUID nodeId : nodeIds) {
            PlanItem item = itemMap.get(nodeId);
            if (item != null) {
                path.add(CriticalPathNode.builder()
                        .issueId(nodeId)
                        .issueKey(item.getIssueKey())
                        .issueTitle(item.getIssueTitle())
                        .durationDays(durations.getOrDefault(nodeId, 0))
                        .earliestStartDays(earliestStart.getOrDefault(nodeId, 0L))
                        .earliestFinishDays(earliestStart.getOrDefault(nodeId, 0L) + durations.getOrDefault(nodeId, 0))
                        .latestStartDays(latestStart.getOrDefault(nodeId, 0L))
                        .latestFinishDays(latestStart.getOrDefault(nodeId, 0L))
                        .floatDays(floatTime.getOrDefault(nodeId, 0L))
                        .isCritical(true)
                        .build());
            }
        }
        return path;
    }

    private Map<UUID, Map<String, Object>> buildFloatAnalysis(Map<UUID, Long> floatTime, Map<UUID, PlanItem> itemMap) {
        Map<UUID, Map<String, Object>> analysis = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : floatTime.entrySet()) {
            UUID nodeId = entry.getKey();
            PlanItem item = itemMap.get(nodeId);
            if (item != null) {
                analysis.put(nodeId, Map.of(
                        "issueKey", item.getIssueKey(),
                        "floatDays", entry.getValue(),
                        "isOnCriticalPath", entry.getValue() == 0
                ));
            }
        }
        return analysis;
    }

    private double calculateRiskScore(boolean affectsCriticalPath, int changeDays, long projectDuration) {
        if (!affectsCriticalPath) {
            return 0.1;
        }
        double criticalImpact = 1.0;
        double sizeImpact = Math.min((double) changeDays / projectDuration, 1.0);
        return (criticalImpact * 0.7) + (sizeImpact * 0.3);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CriticalPathResult {
        @Builder.Default
        private boolean success = true;
        private String message;
        private List<CriticalPathNode> criticalPath;
        @Builder.Default
        private long projectDurationDays = 0;
        @Builder.Default
        private int nodeCount = 0;
        @Builder.Default
        private int criticalNodeCount = 0;
        private Map<UUID, Map<String, Object>> floatAnalysis;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CriticalPathNode {
        private UUID issueId;
        private String issueKey;
        private String issueTitle;
        private int durationDays;
        private long earliestStartDays;
        private long earliestFinishDays;
        private long latestStartDays;
        private long latestFinishDays;
        private long floatDays;
        @Builder.Default
        private boolean isCritical = true;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RiskAnalysis {
        @Builder.Default
        private boolean success = true;
        private String message;
        @Builder.Default
        private boolean affectsCriticalPath = false;
        @Builder.Default
        private long originalProjectDuration = 0;
        @Builder.Default
        private long newProjectDuration = 0;
        @Builder.Default
        private int delayDays = 0;
        @Builder.Default
        private double riskScore = 0;
        private String riskLevel;
        @Builder.Default
        private List<UUID> affectedItemIds = List.of();
        @Builder.Default
        private List<String> recommendations = List.of();
    }
}