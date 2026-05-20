package com.jira.plan.service;

import com.jira.plan.dto.request.CreateDependencyRequest;
import com.jira.plan.dto.response.DependencyResponse;
import com.jira.plan.entity.IssueDependency;
import com.jira.plan.entity.Plan;
import com.jira.plan.exception.ResourceNotFoundException;
import com.jira.plan.repository.IssueDependencyRepository;
import com.jira.plan.repository.PlanRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DependencyService {

    private final IssueDependencyRepository dependencyRepository;
    private final PlanRepository planRepository;
    private final ScheduleEngine scheduleEngine;
    private final CriticalPathService criticalPathService;

    @Transactional(readOnly = true)
    public List<DependencyResponse> getDependenciesByPlanId(UUID planId) {
        return dependencyRepository.findByPlanId(planId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DependencyResponse createDependency(UUID planId, CreateDependencyRequest request) {
        Plan plan = findPlanById(planId);

        if (dependencyRepository.existsByBlockingIssueIdAndBlockedIssueId(
                request.getBlockingIssueId(), request.getBlockedIssueId())) {
            throw new IllegalArgumentException("Dependency already exists");
        }

        // Circular dependency check: if A blocks B, check that B doesn't already block A
        if (dependencyRepository.existsByBlockingIssueIdAndBlockedIssueId(
                request.getBlockedIssueId(), request.getBlockingIssueId())) {
            throw new IllegalArgumentException("Circular dependency detected: issue A cannot block B if B already blocks A");
        }

        // Also check for transitive circular dependencies using DFS
        if (wouldCreateCycle(planId, request.getBlockedIssueId(), request.getBlockingIssueId())) {
            throw new IllegalArgumentException("Circular dependency detected: creating this dependency would form a cycle");
        }

        IssueDependency dependency = IssueDependency.builder()
                .planId(planId)
                .blockingIssueId(request.getBlockingIssueId())
                .blockedIssueId(request.getBlockedIssueId())
                .dependencyType(request.getDependencyType() != null ? request.getDependencyType() : "BLOCKS")
                .build();

        dependency = dependencyRepository.save(dependency);

        // Trigger schedule recalculation for affected items
        propagateScheduleChanges(planId, request.getBlockedIssueId());

        log.info("Created dependency and triggered schedule propagation for plan: {}", planId);
        return toResponse(dependency);
    }

    /**
     * Propagates schedule changes to downstream items when a dependency changes.
     */
    public void propagateScheduleChanges(UUID planId, UUID changedItemId) {
        log.info("Propagating schedule changes for item {} in plan {}", changedItemId, planId);

        List<IssueDependency> dependencies = dependencyRepository.findByPlanId(planId);
        Set<UUID> affectedItems = findDownstreamAffectedItems(changedItemId, dependencies);

        if (!affectedItems.isEmpty()) {
            ScheduleEngine.ScheduleResult result = scheduleEngine.propagateScheduleChanges(planId, changedItemId, 0);
            log.info("Schedule propagation complete: {} items affected", result.getAffectedItemIds().size());
        }
    }

    /**
     * Finds all items that would be affected by a change to the given item.
     * Walks the dependency graph downstream (items that the changed item blocks).
     */
    private Set<UUID> findDownstreamAffectedItems(UUID itemId, List<IssueDependency> dependencies) {
        Set<UUID> affected = new HashSet<>();
        Queue<UUID> toProcess = new LinkedList<>();
        toProcess.offer(itemId);

        Map<UUID, List<UUID>> blockedBy = new HashMap<>();
        for (IssueDependency dep : dependencies) {
            blockedBy.computeIfAbsent(dep.getBlockingIssueId(), k -> new ArrayList<>())
                    .add(dep.getBlockedIssueId());
        }

        while (!toProcess.isEmpty()) {
            UUID current = toProcess.poll();
            List<UUID> blocked = blockedBy.get(current);
            if (blocked != null) {
                for (UUID blockedId : blocked) {
                    if (!affected.contains(blockedId)) {
                        affected.add(blockedId);
                        toProcess.offer(blockedId);
                    }
                }
            }
        }

        return affected;
    }

    /**
     * Gets all items upstream of a given item (items that block this item).
     */
    public Set<UUID> findUpstreamDependencies(UUID itemId, UUID planId) {
        List<IssueDependency> dependencies = dependencyRepository.findByPlanId(planId);
        Set<UUID> upstream = new HashSet<>();
        Queue<UUID> toProcess = new LinkedList<>();
        toProcess.offer(itemId);

        Map<UUID, List<UUID>> blockedBy = new HashMap<>();
        for (IssueDependency dep : dependencies) {
            blockedBy.computeIfAbsent(dep.getBlockedIssueId(), k -> new ArrayList<>())
                    .add(dep.getBlockingIssueId());
        }

        while (!toProcess.isEmpty()) {
            UUID current = toProcess.poll();
            List<UUID> blockers = blockedBy.get(current);
            if (blockers != null) {
                for (UUID blockerId : blockers) {
                    if (!upstream.contains(blockerId)) {
                        upstream.add(blockerId);
                        toProcess.offer(blockerId);
                    }
                }
            }
        }

        return upstream;
    }

    /**
     * Analyzes the impact of removing a dependency.
     */
    public DependencyImpactAnalysis analyzeDependencyImpact(UUID planId, UUID dependencyId) {
        IssueDependency dependency = findDependencyById(dependencyId);

        Set<UUID> upstreamBlockers = findUpstreamDependencies(dependency.getBlockingIssueId(), planId);
        Set<UUID> downstreamBlocked = findDownstreamAffectedItems(dependency.getBlockedIssueId(),
                dependencyRepository.findByPlanId(planId));

        CriticalPathService.CriticalPathResult cpm = criticalPathService.calculateCriticalPath(planId);
        boolean onCriticalPath = cpm.getCriticalPath().stream()
                .anyMatch(node -> node.getIssueId().equals(dependency.getBlockedIssueId()));

        int impactScore = 0;
        if (onCriticalPath) impactScore += 50;
        impactScore += Math.min(downstreamBlocked.size() * 5, 30);
        impactScore += Math.min(upstreamBlockers.size() * 5, 20);

        String impactLevel = impactScore > 60 ? "HIGH" : impactScore > 30 ? "MEDIUM" : "LOW";

        return DependencyImpactAnalysis.builder()
                .dependencyId(dependencyId)
                .blockingIssueId(dependency.getBlockingIssueId())
                .blockedIssueId(dependency.getBlockedIssueId())
                .onCriticalPath(onCriticalPath)
                .upstreamDependencyCount(upstreamBlockers.size())
                .downstreamDependencyCount(downstreamBlocked.size())
                .impactScore(impactScore)
                .impactLevel(impactLevel)
                .affectedItemIds(new ArrayList<>(downstreamBlocked))
                .build();
    }

    /**
     * Detects if adding a new edge (blockedId -> blockingId) would create a cycle.
     * Uses DFS to detect if there's already a path from blockingId to blockedId.
     */
    private boolean wouldCreateCycle(UUID planId, UUID blockedIssueId, UUID blockingIssueId) {
        Set<UUID> visited = new java.util.HashSet<>();
        Set<UUID> recursionStack = new java.util.HashSet<>();
        return hasPath(planId, blockingIssueId, blockedIssueId, visited, recursionStack);
    }

    private boolean hasPath(UUID planId, UUID current, UUID target, Set<UUID> visited, Set<UUID> recursionStack) {
        if (recursionStack.contains(current)) return true;  // Cycle detected
        if (visited.contains(current)) return false;

        visited.add(current);
        recursionStack.add(current);

        // Find all issues that CURRENT blocks (current -> next)
        List<IssueDependency> dependencies = dependencyRepository.findByPlanIdAndBlockingIssueId(planId, current);
        for (IssueDependency dep : dependencies) {
            if (dep.getBlockedIssueId().equals(target)) return true;  // Path found: current -> target
            if (hasPath(planId, dep.getBlockedIssueId(), target, visited, recursionStack)) return true;
        }

        recursionStack.remove(current);
        return false;
    }

    @Transactional
    public void deleteDependency(UUID planId, UUID dependencyId) {
        IssueDependency dependency = findDependencyById(dependencyId);
        UUID blockedIssueId = dependency.getBlockedIssueId();
        dependencyRepository.delete(dependency);

        // Trigger schedule recalculation after dependency removal
        propagateScheduleChanges(planId, blockedIssueId);

        log.info("Deleted dependency and triggered schedule propagation for plan: {}", planId);
    }

    private Plan findPlanById(UUID id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));
    }

    private IssueDependency findDependencyById(UUID id) {
        return dependencyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dependency", "id", id));
    }

    private DependencyResponse toResponse(IssueDependency dependency) {
        return DependencyResponse.builder()
                .id(dependency.getId())
                .planId(dependency.getPlanId())
                .blockingIssueId(dependency.getBlockingIssueId())
                .blockedIssueId(dependency.getBlockedIssueId())
                .dependencyType(dependency.getDependencyType())
                .createdAt(dependency.getCreatedAt())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DependencyImpactAnalysis {
        private UUID dependencyId;
        private UUID blockingIssueId;
        private UUID blockedIssueId;
        @Builder.Default
        private boolean onCriticalPath = false;
        @Builder.Default
        private int upstreamDependencyCount = 0;
        @Builder.Default
        private int downstreamDependencyCount = 0;
        @Builder.Default
        private int impactScore = 0;
        private String impactLevel;
        @Builder.Default
        private List<UUID> affectedItemIds = List.of();
    }
}
