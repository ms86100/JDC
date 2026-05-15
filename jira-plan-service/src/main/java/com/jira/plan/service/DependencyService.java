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

        IssueDependency dependency = IssueDependency.builder()
                .planId(planId)
                .blockingIssueId(request.getBlockingIssueId())
                .blockedIssueId(request.getBlockedIssueId())
                .dependencyType(request.getDependencyType() != null ? request.getDependencyType() : "BLOCKS")
                .build();

        dependency = dependencyRepository.save(dependency);
        return toResponse(dependency);
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
