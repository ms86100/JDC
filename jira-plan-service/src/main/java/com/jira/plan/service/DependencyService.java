package com.jira.plan.service;

import com.jira.plan.dto.request.CreateDependencyRequest;
import com.jira.plan.dto.response.DependencyResponse;
import com.jira.plan.entity.IssueDependency;
import com.jira.plan.entity.Plan;
import com.jira.plan.exception.ResourceNotFoundException;
import com.jira.plan.repository.IssueDependencyRepository;
import com.jira.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DependencyService {

    private final IssueDependencyRepository dependencyRepository;
    private final PlanRepository planRepository;

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
        return toResponse(dependency);
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
        dependencyRepository.delete(dependency);
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
}
