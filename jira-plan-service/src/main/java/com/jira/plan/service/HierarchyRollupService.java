package com.jira.plan.service;

import com.jira.plan.entity.PlanItem;
import com.jira.plan.repository.PlanItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HierarchyRollupService {

    private final PlanItemRepository planItemRepository;

    public HierarchyMetrics calculateRollup(UUID planId) {
        log.debug("Calculating hierarchy rollup for plan: {}", planId);

        List<PlanItem> items = planItemRepository.findByPlanIdAndIsActiveTrue(planId);

        if (items.isEmpty()) {
            return HierarchyMetrics.empty();
        }

        Map<UUID, List<PlanItem>> childrenByParent = items.stream()
                .filter(item -> item.getParentId() != null)
                .collect(Collectors.groupingBy(PlanItem::getParentId));

        Set<UUID> parentIds = childrenByParent.keySet();
        List<PlanItem> parentItems = items.stream()
                .filter(item -> parentIds.contains(item.getId()))
                .toList();

        int totalPoints = 0;
        int completedPoints = 0;
        int inProgressPoints = 0;
        int todoPoints = 0;

        for (PlanItem item : parentItems) {
            HierarchyMetrics childMetrics = rollupChildren(item.getId(), childrenByParent);

            totalPoints += childMetrics.getTotalPoints();
            completedPoints += childMetrics.getCompletedPoints();
            inProgressPoints += childMetrics.getInProgressPoints();
            todoPoints += childMetrics.getTodoPoints();
        }

        for (PlanItem item : items) {
            if (item.getParentId() == null && !parentIds.contains(item.getId())) {
                if (item.getStoryPoints() != null) {
                    totalPoints += item.getStoryPoints();
                    if ("DONE".equals(item.getStatusCategory())) {
                        completedPoints += item.getStoryPoints();
                    } else if ("IN_PROGRESS".equals(item.getStatusCategory())) {
                        inProgressPoints += item.getStoryPoints();
                    } else {
                        todoPoints += item.getStoryPoints();
                    }
                }
            }
        }

        double progress = totalPoints > 0 ? (completedPoints * 100.0) / totalPoints : 0.0;

        return HierarchyMetrics.builder()
                .totalPoints(totalPoints)
                .completedPoints(completedPoints)
                .inProgressPoints(inProgressPoints)
                .todoPoints(todoPoints)
                .progressPercentage(progress)
                .issueCount(items.size())
                .build();
    }

    private HierarchyMetrics rollupChildren(UUID parentId, Map<UUID, List<PlanItem>> childrenByParent) {
        List<PlanItem> children = childrenByParent.getOrDefault(parentId, List.of());

        int totalPoints = 0;
        int completedPoints = 0;
        int inProgressPoints = 0;
        int todoPoints = 0;

        for (PlanItem child : children) {
            if (childrenByParent.containsKey(child.getId())) {
                HierarchyMetrics childMetrics = rollupChildren(child.getId(), childrenByParent);
                totalPoints += childMetrics.getTotalPoints();
                completedPoints += childMetrics.getCompletedPoints();
                inProgressPoints += childMetrics.getInProgressPoints();
                todoPoints += childMetrics.getTodoPoints();
            } else {
                if (child.getStoryPoints() != null) {
                    totalPoints += child.getStoryPoints();
                    if ("DONE".equals(child.getStatusCategory())) {
                        completedPoints += child.getStoryPoints();
                    } else if ("IN_PROGRESS".equals(child.getStatusCategory())) {
                        inProgressPoints += child.getStoryPoints();
                    } else {
                        todoPoints += child.getStoryPoints();
                    }
                }
            }
        }

        return HierarchyMetrics.builder()
                .totalPoints(totalPoints)
                .completedPoints(completedPoints)
                .inProgressPoints(inProgressPoints)
                .todoPoints(todoPoints)
                .progressPercentage(totalPoints > 0 ? (completedPoints * 100.0) / totalPoints : 0.0)
                .build();
    }

    public Map<String, Object> getHierarchyTree(UUID planId) {
        log.debug("Building hierarchy tree for plan: {}", planId);

        List<PlanItem> items = planItemRepository.findByPlanIdAndIsActiveTrue(planId);

        Map<UUID, List<PlanItem>> childrenByParent = items.stream()
                .filter(item -> item.getParentId() != null)
                .collect(Collectors.groupingBy(PlanItem::getParentId));

        Set<UUID> rootIds = items.stream()
                .map(item -> item.getParentId() != null ? item.getParentId() : item.getId())
                .collect(Collectors.toSet());

        List<Map<String, Object>> roots = items.stream()
                .filter(item -> !items.stream()
                        .anyMatch(other -> other.getParentId() != null && other.getParentId().equals(item.getId())))
                .map(item -> buildTreeNode(item, childrenByParent))
                .collect(Collectors.toList());

        HierarchyMetrics metrics = calculateRollup(planId);

        return Map.of(
                "tree", roots,
                "metrics", metrics
        );
    }

    private Map<String, Object> buildTreeNode(PlanItem item, Map<UUID, List<PlanItem>> childrenByParent) {
        List<PlanItem> children = childrenByParent.get(item.getId());

        List<Map<String, Object>> childNodes = Collections.emptyList();
        if (children != null && !children.isEmpty()) {
            childNodes = children.stream()
                    .map(child -> buildTreeNode(child, childrenByParent))
                    .collect(Collectors.toList());
        }

        int subtreePoints = calculateSubtreePoints(item, childrenByParent);
        int completedSubtreePoints = calculateCompletedSubtreePoints(item, childrenByParent);
        double progress = subtreePoints > 0 ? (completedSubtreePoints * 100.0) / subtreePoints : 0.0;

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", item.getId());
        node.put("issueId", item.getIssueId());
        node.put("issueKey", item.getIssueKey());
        node.put("issueTitle", item.getIssueTitle());
        node.put("issueType", item.getIssueType());
        node.put("storyPoints", item.getStoryPoints());
        node.put("status", item.getStatus());
        node.put("statusCategory", item.getStatusCategory());
        node.put("targetDate", item.getTargetDate());
        node.put("subtreePoints", subtreePoints);
        node.put("completedSubtreePoints", completedSubtreePoints);
        node.put("subtreeProgress", progress);
        node.put("children", childNodes);

        return node;
    }

    private int calculateSubtreePoints(PlanItem item, Map<UUID, List<PlanItem>> childrenByParent) {
        List<PlanItem> children = childrenByParent.get(item.getId());

        if (children == null || children.isEmpty()) {
            return item.getStoryPoints() != null ? item.getStoryPoints() : 0;
        }

        int total = item.getStoryPoints() != null ? item.getStoryPoints() : 0;
        for (PlanItem child : children) {
            total += calculateSubtreePoints(child, childrenByParent);
        }
        return total;
    }

    private int calculateCompletedSubtreePoints(PlanItem item, Map<UUID, List<PlanItem>> childrenByParent) {
        List<PlanItem> children = childrenByParent.get(item.getId());

        int completed = 0;
        if ("DONE".equals(item.getStatusCategory()) && item.getStoryPoints() != null) {
            completed = item.getStoryPoints();
        }

        if (children != null) {
            for (PlanItem child : children) {
                completed += calculateCompletedSubtreePoints(child, childrenByParent);
            }
        }

        return completed;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HierarchyMetrics {
        private int totalPoints;
        private int completedPoints;
        private int inProgressPoints;
        private int todoPoints;
        private double progressPercentage;
        @lombok.Builder.Default
        private int issueCount = 0;

        public static HierarchyMetrics empty() {
            return HierarchyMetrics.builder()
                    .totalPoints(0)
                    .completedPoints(0)
                    .inProgressPoints(0)
                    .todoPoints(0)
                    .progressPercentage(0.0)
                    .issueCount(0)
                    .build();
        }
    }
}