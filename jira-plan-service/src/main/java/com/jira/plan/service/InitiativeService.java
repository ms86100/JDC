package com.jira.plan.service;

import com.jira.plan.dto.InitiativeRequest;
import com.jira.plan.dto.InitiativeResponse;
import com.jira.plan.entity.Initiative;
import com.jira.plan.entity.InitiativeEpic;
import com.jira.plan.entity.InitiativePlan;
import com.jira.plan.entity.Plan;
import com.jira.plan.exception.DuplicateResourceException;
import com.jira.plan.exception.ResourceNotFoundException;
import com.jira.plan.repository.InitiativeEpicRepository;
import com.jira.plan.repository.InitiativePlanRepository;
import com.jira.plan.repository.InitiativeRepository;
import com.jira.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InitiativeService {

    private final InitiativeRepository initiativeRepository;
    private final InitiativeEpicRepository initiativeEpicRepository;
    private final InitiativePlanRepository initiativePlanRepository;
    private final PlanRepository planRepository;
    private final HierarchyRollupService hierarchyRollupService;

    @Transactional
    public InitiativeResponse createInitiative(InitiativeRequest request, UUID currentUserId) {
        log.info("Creating initiative: {} by user: {}", request.getName(), currentUserId);

        if (initiativeRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Initiative with name '" + request.getName() + "' already exists");
        }

        Initiative initiative = Initiative.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(request.getOwnerId() != null ? request.getOwnerId() : currentUserId)
                .programId(request.getProgramId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .targetDate(request.getTargetDate())
                .color(request.getColor())
                .avatarUrl(request.getAvatarUrl())
                .isActive(true)
                .build();

        initiative = initiativeRepository.save(initiative);
        log.info("Initiative created with id: {}", initiative.getId());

        return mapToResponse(initiative);
    }

    @Transactional(readOnly = true)
    public InitiativeResponse getInitiative(UUID initiativeId) {
        Initiative initiative = findInitiativeOrThrow(initiativeId);
        return mapToResponse(initiative);
    }

    @Transactional(readOnly = true)
    public List<InitiativeResponse> getAllInitiatives() {
        return initiativeRepository.findAllActiveOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InitiativeResponse> getInitiativesByProgram(UUID programId) {
        return initiativeRepository.findByProgramId(programId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public InitiativeResponse updateInitiative(UUID initiativeId, InitiativeRequest request) {
        log.info("Updating initiative: {}", initiativeId);

        Initiative initiative = findInitiativeOrThrow(initiativeId);

        if (request.getName() != null && !request.getName().equals(initiative.getName())) {
            if (initiativeRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), initiativeId)) {
                throw new DuplicateResourceException("Initiative with name '" + request.getName() + "' already exists");
            }
            initiative.setName(request.getName());
        }

        if (request.getDescription() != null) {
            initiative.setDescription(request.getDescription());
        }
        if (request.getOwnerId() != null) {
            initiative.setOwnerId(request.getOwnerId());
        }
        if (request.getProgramId() != null) {
            initiative.setProgramId(request.getProgramId());
        }
        if (request.getStartDate() != null) {
            initiative.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            initiative.setEndDate(request.getEndDate());
        }
        if (request.getTargetDate() != null) {
            initiative.setTargetDate(request.getTargetDate());
        }
        if (request.getColor() != null) {
            initiative.setColor(request.getColor());
        }
        if (request.getAvatarUrl() != null) {
            initiative.setAvatarUrl(request.getAvatarUrl());
        }

        initiative = initiativeRepository.save(initiative);
        log.info("Initiative updated: {}", initiativeId);

        return mapToResponse(initiative);
    }

    @Transactional
    public void deleteInitiative(UUID initiativeId) {
        log.info("Deleting initiative: {}", initiativeId);

        Initiative initiative = findInitiativeOrThrow(initiativeId);
        initiative.setIsActive(false);
        initiativeRepository.save(initiative);

        log.info("Initiative soft-deleted: {}", initiativeId);
    }

    @Transactional
    public InitiativeResponse addEpicToInitiative(UUID initiativeId, UUID epicId, String epicKey, String epicName) {
        log.info("Adding epic {} to initiative {}", epicId, initiativeId);

        Initiative initiative = findInitiativeOrThrow(initiativeId);

        if (initiativeEpicRepository.existsByInitiativeIdAndEpicId(initiativeId, epicId)) {
            throw new DuplicateResourceException("Epic " + epicKey + " is already in this initiative");
        }

        int maxSequence = initiativeEpicRepository.findByInitiativeIdOrderBySequenceAsc(initiativeId)
                .stream()
                .mapToInt(InitiativeEpic::getSequence)
                .max()
                .orElse(-1);

        InitiativeEpic initiativeEpic = InitiativeEpic.builder()
                .initiativeId(initiativeId)
                .epicId(epicId)
                .epicKey(epicKey)
                .epicName(epicName)
                .sequence(maxSequence + 1)
                .build();

        initiativeEpicRepository.save(initiativeEpic);

        recalculateInitiativeProgress(initiativeId);

        return mapToResponse(initiativeRepository.findById(initiativeId).orElseThrow());
    }

    @Transactional
    public void removeEpicFromInitiative(UUID initiativeId, UUID epicId) {
        log.info("Removing epic {} from initiative {}", epicId, initiativeId);

        InitiativeEpic initiativeEpic = initiativeEpicRepository.findByInitiativeIdAndEpicId(initiativeId, epicId)
                .orElseThrow(() -> new ResourceNotFoundException("Epic not found in initiative"));

        initiativeEpicRepository.delete(initiativeEpic);

        recalculateInitiativeProgress(initiativeId);
    }

    @Transactional
    public InitiativeResponse addPlanToInitiative(UUID initiativeId, UUID planId) {
        log.info("Adding plan {} to initiative {}", planId, initiativeId);

        Initiative initiative = findInitiativeOrThrow(initiativeId);

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", planId));

        if (initiativePlanRepository.existsByInitiativeIdAndPlanId(initiativeId, planId)) {
            throw new DuplicateResourceException("Plan is already in this initiative");
        }

        int maxSequence = initiativePlanRepository.findByInitiativeIdOrderBySequenceAsc(initiativeId)
                .stream()
                .mapToInt(InitiativePlan::getSequence)
                .max()
                .orElse(-1);

        InitiativePlan initiativePlan = InitiativePlan.builder()
                .initiativeId(initiativeId)
                .planId(planId)
                .sequence(maxSequence + 1)
                .build();

        initiativePlanRepository.save(initiativePlan);

        return mapToResponse(initiativeRepository.findById(initiativeId).orElseThrow());
    }

    @Transactional
    public void removePlanFromInitiative(UUID initiativeId, UUID planId) {
        log.info("Removing plan {} from initiative {}", planId, initiativeId);

        initiativePlanRepository.deleteByInitiativeIdAndPlanId(initiativeId, planId);
    }

    @Transactional
    public void recalculateInitiativeProgress(UUID initiativeId) {
        log.debug("Recalculating progress for initiative: {}", initiativeId);

        Initiative initiative = findInitiativeOrThrow(initiativeId);

        Integer totalPoints = initiativeEpicRepository.sumTotalStoryPointsByInitiativeId(initiativeId);
        Integer completedPoints = initiativeEpicRepository.sumCompletedStoryPointsByInitiativeId(initiativeId);

        initiative.setTotalStoryPoints(totalPoints != null ? totalPoints : 0);
        initiative.setCompletedStoryPoints(completedPoints != null ? completedPoints : 0);
        initiative.recalculateProgress();

        initiativeRepository.save(initiative);
    }

    public Initiative findInitiativeOrThrow(UUID initiativeId) {
        return initiativeRepository.findById(initiativeId)
                .orElseThrow(() -> new ResourceNotFoundException("Initiative", "id", initiativeId));
    }

    private InitiativeResponse mapToResponse(Initiative initiative) {
        List<InitiativeEpic> epics = initiativeEpicRepository.findByInitiativeIdOrderBySequenceAsc(initiative.getId());

        return InitiativeResponse.builder()
                .id(initiative.getId())
                .name(initiative.getName())
                .description(initiative.getDescription())
                .ownerId(initiative.getOwnerId())
                .programId(initiative.getProgramId())
                .startDate(initiative.getStartDate())
                .endDate(initiative.getEndDate())
                .targetDate(initiative.getTargetDate())
                .totalStoryPoints(initiative.getTotalStoryPoints())
                .completedStoryPoints(initiative.getCompletedStoryPoints())
                .progressPercentage(initiative.getProgressPercentage())
                .color(initiative.getColor())
                .avatarUrl(initiative.getAvatarUrl())
                .isActive(initiative.getIsActive())
                .createdAt(initiative.getCreatedAt())
                .updatedAt(initiative.getUpdatedAt())
                .epics(epics.stream().map(this::mapEpicToResponse).collect(Collectors.toList()))
                .build();
    }

    private InitiativeResponse.EpicProgress mapEpicToResponse(InitiativeEpic epic) {
        return InitiativeResponse.EpicProgress.builder()
                .epicId(epic.getEpicId())
                .epicKey(epic.getEpicKey())
                .epicName(epic.getEpicName())
                .totalStoryPoints(epic.getTotalStoryPoints())
                .completedStoryPoints(epic.getCompletedStoryPoints())
                .progressPercentage(epic.getProgressPercentage())
                .startDate(epic.getStartDate())
                .endDate(epic.getEndDate())
                .status(epic.getStatus())
                .build();
    }
}