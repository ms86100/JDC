package com.jira.plan.service;

import com.jira.plan.dto.request.CreatePlanRequest;
import com.jira.plan.dto.request.UpdatePlanRequest;
import com.jira.plan.dto.response.PlanResponse;
import com.jira.plan.entity.Plan;
import com.jira.plan.exception.OptimisticLockException;
import com.jira.plan.exception.ResourceNotFoundException;
import com.jira.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;

    @Transactional(readOnly = true)
    public List<PlanResponse> getAllPlans() {
        return planRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> getPlansByProgramId(UUID programId) {
        return planRepository.findByProgramId(programId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlanResponse getPlanById(UUID id) {
        Plan plan = findPlanById(id);
        return toResponse(plan);
    }

    @Transactional
    public PlanResponse createPlan(CreatePlanRequest request, UUID userId) {
        Plan plan = Plan.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(request.getOwnerId() != null ? request.getOwnerId() : userId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .settings(request.getSettings() != null ? request.getSettings() : Map.of())
                .build();
        plan = planRepository.save(plan);
        return toResponse(plan);
    }

    @Transactional
    public PlanResponse updatePlan(UUID id, UpdatePlanRequest request) {
        Plan plan = findPlanById(id);

        // Optimistic locking: check version if provided
        if (request.getVersion() != null && !request.getVersion().equals(plan.getVersion())) {
            throw new OptimisticLockException(
                "Plan was modified by another user. Please refresh and try again. " +
                "Expected version: " + plan.getVersion() + ", provided: " + request.getVersion()
            );
        }

        if (request.getName() != null) {
            plan.setName(request.getName());
        }
        if (request.getDescription() != null) {
            plan.setDescription(request.getDescription());
        }
        if (request.getOwnerId() != null) {
            plan.setOwnerId(request.getOwnerId());
        }
        if (request.getStartDate() != null) {
            plan.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            plan.setEndDate(request.getEndDate());
        }
        if (request.getIsActive() != null) {
            plan.setIsActive(request.getIsActive());
        }
        if (request.getSettings() != null) {
            plan.setSettings(request.getSettings());
        }
        plan = planRepository.save(plan);
        return toResponse(plan);
    }

    @Transactional
    public PlanResponse updatePlanSettings(UUID id, Map<String, Object> settings) {
        Plan plan = findPlanById(id);
        plan.setSettings(settings);
        plan = planRepository.save(plan);
        return toResponse(plan);
    }

    @Transactional
    public void deletePlan(UUID id) {
        Plan plan = findPlanById(id);
        plan.setIsActive(false);

        // Cascade soft-delete to related entities to maintain referential integrity
        if (plan.getItems() != null) {
            plan.getItems().forEach(item -> item.setIsActive(false));
        }
        if (plan.getTeams() != null) {
            plan.getTeams().forEach(team -> team.setIsActive(false));
        }
        if (plan.getReleases() != null) {
            plan.getReleases().forEach(release -> release.setIsActive(false));
        }

        planRepository.save(plan);
    }

    private Plan findPlanById(UUID id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));
    }

    private PlanResponse toResponse(Plan plan) {
        return PlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .ownerId(plan.getOwnerId())
                .settings(plan.getSettings())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .isActive(plan.getIsActive())
                .version(plan.getVersion())
                .itemCount(plan.getItems() != null ? plan.getItems().size() : 0)
                .teamCount(plan.getTeams() != null ? plan.getTeams().size() : 0)
                .releaseCount(plan.getReleases() != null ? plan.getReleases().size() : 0)
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}
