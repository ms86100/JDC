package com.avionics_systems.plan.service;

import com.avionics_systems.plan.dto.CreateGoalRequest;
import com.avionics_systems.plan.dto.GoalResponse;
import com.avionics_systems.plan.entity.PlanGoal;
import com.avionics_systems.plan.exception.ResourceNotFoundException;
import com.avionics_systems.plan.repository.PlanGoalRepository;
import com.avionics_systems.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalService {

    private final PlanGoalRepository goalRepository;
    private final PlanRepository planRepository;

    @Value("${app.goal.status.not-started:NOT_STARTED}")
    private String goalStatusNotStarted;

    @Value("${app.goal.status.in-progress:IN_PROGRESS}")
    private String goalStatusInProgress;

    @Value("${app.goal.status.completed:COMPLETED}")
    private String goalStatusCompleted;

    @Transactional(readOnly = true)
    public List<GoalResponse> getGoalsByPlanId(UUID planId) {
        List<PlanGoal> goals = goalRepository.findByPlanId(planId);
        return goals.stream()
                .map(this::toGoalResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GoalResponse getGoalById(UUID planId, UUID goalId) {
        PlanGoal goal = findGoalById(goalId);
        if (!goal.getPlanId().equals(planId)) {
            throw new ResourceNotFoundException("Goal", "id", goalId);
        }
        return toGoalResponse(goal);
    }

    @Transactional
    public GoalResponse createGoal(UUID planId, CreateGoalRequest request) {
        planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", planId));

        PlanGoal goal = PlanGoal.builder()
                .planId(planId)
                .name(request.getName())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : goalStatusNotStarted)
                .targetDate(request.getTargetDate())
                .parentGoalId(request.getParentGoalId())
                .linkedEpicIds(request.getLinkedEpicIds() != null
                        ? request.getLinkedEpicIds().toArray(new String[0])
                        : null)
                .color(request.getColor())
                .ownerUserId(request.getOwnerUserId())
                .build();

        goal = goalRepository.save(goal);
        log.info("Created goal '{}' for plan {}", goal.getName(), planId);
        return toGoalResponse(goal);
    }

    @Transactional
    public GoalResponse updateGoal(UUID planId, UUID goalId, CreateGoalRequest request) {
        PlanGoal goal = findGoalById(goalId);
        if (!goal.getPlanId().equals(planId)) {
            throw new ResourceNotFoundException("Goal", "id", goalId);
        }

        if (request.getName() != null) {
            goal.setName(request.getName());
        }
        if (request.getDescription() != null) {
            goal.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            goal.setStatus(request.getStatus());
        }
        if (request.getTargetDate() != null) {
            goal.setTargetDate(request.getTargetDate());
        }
        if (request.getParentGoalId() != null) {
            goal.setParentGoalId(request.getParentGoalId());
        }
        if (request.getLinkedEpicIds() != null) {
            goal.setLinkedEpicIds(request.getLinkedEpicIds().toArray(new String[0]));
        }
        if (request.getColor() != null) {
            goal.setColor(request.getColor());
        }
        if (request.getOwnerUserId() != null) {
            goal.setOwnerUserId(request.getOwnerUserId());
        }

        goal = goalRepository.save(goal);
        log.info("Updated goal '{}' for plan {}", goal.getName(), planId);
        return toGoalResponse(goal);
    }

    @Transactional
    public void deleteGoal(UUID planId, UUID goalId) {
        PlanGoal goal = findGoalById(goalId);
        if (!goal.getPlanId().equals(planId)) {
            throw new ResourceNotFoundException("Goal", "id", goalId);
        }
        goalRepository.delete(goal);
        log.info("Deleted goal {} from plan {}", goalId, planId);
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> getGoalChildren(UUID planId, UUID goalId) {
        PlanGoal goal = findGoalById(goalId);
        if (!goal.getPlanId().equals(planId)) {
            throw new ResourceNotFoundException("Goal", "id", goalId);
        }
        List<PlanGoal> children = goalRepository.findByParentGoalId(goalId);
        return children.stream()
                .map(this::toGoalResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> getGoalHierarchy(UUID planId) {
        List<PlanGoal> allGoals = goalRepository.findByPlanId(planId);
        Map<UUID, List<PlanGoal>> childrenMap = allGoals.stream()
                .filter(g -> g.getParentGoalId() != null)
                .collect(Collectors.groupingBy(PlanGoal::getParentGoalId));

        List<PlanGoal> rootGoals = allGoals.stream()
                .filter(g -> g.getParentGoalId() == null)
                .collect(Collectors.toList());

        return rootGoals.stream()
                .map(g -> toGoalResponseWithChildren(g, childrenMap))
                .collect(Collectors.toList());
    }

    @Transactional
    public GoalResponse calculateProgress(UUID planId, UUID goalId) {
        PlanGoal goal = findGoalById(goalId);
        if (!goal.getPlanId().equals(planId)) {
            throw new ResourceNotFoundException("Goal", "id", goalId);
        }

        // Calculate progress from child goals if any
        List<PlanGoal> children = goalRepository.findByParentGoalId(goalId);
        if (!children.isEmpty()) {
            int totalProgress = children.stream()
                    .mapToInt(c -> c.getProgress() != null ? c.getProgress() : 0)
                    .sum();
            goal.setProgress(totalProgress / children.size());
        }

        // Update status based on progress
        if (goal.getProgress() != null) {
            if (goal.getProgress() >= 100) {
                goal.setStatus(goalStatusCompleted);
            } else if (goal.getProgress() > 0) {
                goal.setStatus(goalStatusInProgress);
            }
        }

        goal = goalRepository.save(goal);
        log.info("Calculated progress for goal {}: {}%", goalId, goal.getProgress());
        return toGoalResponse(goal);
    }

    private PlanGoal findGoalById(UUID id) {
        return goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", "id", id));
    }

    private GoalResponse toGoalResponse(PlanGoal goal) {
        return GoalResponse.builder()
                .id(goal.getId())
                .planId(goal.getPlanId())
                .name(goal.getName())
                .description(goal.getDescription())
                .status(goal.getStatus())
                .targetDate(goal.getTargetDate())
                .progress(goal.getProgress())
                .parentGoalId(goal.getParentGoalId())
                .linkedEpicIds(goal.getLinkedEpicIds() != null
                        ? Arrays.asList(goal.getLinkedEpicIds())
                        : List.of())
                .color(goal.getColor())
                .ownerUserId(goal.getOwnerUserId())
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .build();
    }

    private GoalResponse toGoalResponseWithChildren(PlanGoal goal, Map<UUID, List<PlanGoal>> childrenMap) {
        GoalResponse response = toGoalResponse(goal);
        List<PlanGoal> children = childrenMap.getOrDefault(goal.getId(), List.of());
        response.setChildren(children.stream()
                .map(c -> toGoalResponseWithChildren(c, childrenMap))
                .collect(Collectors.toList()));
        return response;
    }
}
