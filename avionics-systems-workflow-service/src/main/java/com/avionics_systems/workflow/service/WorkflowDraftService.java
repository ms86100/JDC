package com.avionics_systems.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.workflow.dto.*;
import com.avionics_systems.workflow.entity.*;
import com.avionics_systems.workflow.exception.ResourceNotFoundException;
import com.avionics_systems.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowDraftService {

    private final WorkflowDraftRepository workflowDraftRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final WorkflowStatusRepository workflowStatusRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowConditionRepository workflowConditionRepository;
    private final WorkflowValidatorRepository workflowValidatorRepository;
    private final WorkflowPostFunctionRepository workflowPostFunctionRepository;
    private final WorkflowTransitionPropertyRepository workflowTransitionPropertyRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public WorkflowDraftResponse createDraft(UUID workflowId, UUID userId) {
        log.info("Creating draft for workflow: {}", workflowId);

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", workflowId));

        Optional<WorkflowDraft> existingDraft = workflowDraftRepository
                .findByWorkflowIdAndDraftStatusActive(workflowId);
        if (existingDraft.isPresent()) {
            log.info("Active draft already exists for workflow: {}", workflowId);
            return mapToResponse(existingDraft.get(), workflow);
        }

        Map<String, Object> draftData = buildDraftData(workflow);
        String draftDataJson;
        try {
            draftDataJson = objectMapper.writeValueAsString(draftData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize draft data", e);
        }

        int nextVersion = getNextDraftVersion(workflowId);

        WorkflowDraft draft = WorkflowDraft.builder()
                .workflowId(workflowId)
                .name(workflow.getName() + " (Draft)")
                .description(workflow.getDescription())
                .draftData(draftDataJson)
                .parentVersion(nextVersion)
                .createdBy(userId)
                .isDraftOfPublished(!workflow.getIsDraft())
                .draftStatus(WorkflowDraft.STATUS_ACTIVE)
                .build();

        draft = workflowDraftRepository.save(draft);
        workflow.lock(userId);
        workflowRepository.save(workflow);

        log.info("Draft created: {}", draft.getId());
        return mapToResponse(draft, workflow);
    }

    @Transactional(readOnly = true)
    public WorkflowDraftResponse getDraft(UUID draftId) {
        WorkflowDraft draft = workflowDraftRepository.findById(draftId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDraft", "id", draftId));

        Workflow workflow = workflowRepository.findById(draft.getWorkflowId())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", draft.getWorkflowId()));

        return mapToResponse(draft, workflow);
    }

    @Transactional(readOnly = true)
    public WorkflowDraftResponse getDraftForWorkflow(UUID workflowId) {
        WorkflowDraft draft = workflowDraftRepository.findByWorkflowIdAndDraftStatusActive(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("No active draft for workflow: " + workflowId));

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", workflowId));

        return mapToResponse(draft, workflow);
    }

    @Transactional
    public WorkflowDraftResponse updateDraft(UUID draftId, String draftData, UUID userId) {
        log.info("Updating draft: {}", draftId);

        WorkflowDraft draft = workflowDraftRepository.findById(draftId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDraft", "id", draftId));

        draft.setDraftData(draftData);
        final WorkflowDraft savedDraft = workflowDraftRepository.save(draft);

        Workflow workflow = workflowRepository.findById(savedDraft.getWorkflowId())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", savedDraft.getWorkflowId()));

        return mapToResponse(savedDraft, workflow);
    }

    @Transactional
    public WorkflowResponse publishDraft(UUID draftId, UUID userId, String changeDescription) {
        log.info("Publishing draft: {}", draftId);

        WorkflowDraft draft = workflowDraftRepository.findById(draftId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDraft", "id", draftId));

        if (!WorkflowDraft.STATUS_ACTIVE.equals(draft.getDraftStatus())) {
            throw new IllegalStateException("Draft is not active");
        }

        Workflow workflow = workflowRepository.findById(draft.getWorkflowId())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", draft.getWorkflowId()));

        int nextVersion = getNextWorkflowVersion(workflow.getId());
        createVersionSnapshot(workflow, nextVersion, changeDescription, userId);

        applyDraftChanges(workflow, draft.getDraftData());

        workflow.setIsDraft(false);
        workflow.setPublishedAt(LocalDateTime.now());
        workflow.setUpdatedBy(userId);
        workflow.unlock();
        workflow = workflowRepository.save(workflow);

        draft.setDraftStatus(WorkflowDraft.STATUS_PUBLISHED);
        workflowDraftRepository.save(draft);

        log.info("Draft {} published as version {}", draftId, nextVersion);
        return mapWorkflowToResponse(workflow);
    }

    @Transactional
    public void discardDraft(UUID draftId, UUID userId) {
        log.info("Discarding draft: {}", draftId);

        WorkflowDraft draft = workflowDraftRepository.findById(draftId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowDraft", "id", draftId));

        Workflow workflow = workflowRepository.findById(draft.getWorkflowId())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", draft.getWorkflowId()));

        workflow.unlock();
        workflowRepository.save(workflow);

        draft.setDraftStatus(WorkflowDraft.STATUS_DISCARDED);
        workflowDraftRepository.save(draft);

        log.info("Draft discarded: {}", draftId);
    }

    @Transactional(readOnly = true)
    public List<WorkflowDraftResponse> getDraftsForWorkflow(UUID workflowId) {
        return workflowDraftRepository.findByWorkflowId(workflowId).stream()
                .map(draft -> {
                    Workflow workflow = workflowRepository.findById(draft.getWorkflowId()).orElse(null);
                    return mapToResponse(draft, workflow);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkflowVersionResponse> getVersionHistory(UUID workflowId) {
        return workflowVersionRepository.findByWorkflowIdOrderByVersionNumberDesc(workflowId).stream()
                .map(this::mapVersionToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkflowVersionResponse getVersion(UUID versionId) {
        WorkflowVersion version = workflowVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowVersion", "id", versionId));
        return mapVersionToResponse(version);
    }

    @Transactional
    public WorkflowResponse rollbackToVersion(UUID workflowId, Integer versionNumber, UUID userId) {
        log.info("Rolling back workflow {} to version {}", workflowId, versionNumber);

        WorkflowVersion targetVersion = workflowVersionRepository
                .findByWorkflowIdAndVersionNumber(workflowId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + versionNumber + " not found for workflow: " + workflowId));

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", workflowId));

        int nextVersion = getNextWorkflowVersion(workflowId);
        createVersionSnapshot(workflow, nextVersion, "Before rollback", userId);

        String snapshotData = targetVersion.getWorkflowSnapshot();
        applyDraftChanges(workflow, snapshotData);

        workflow.setUpdatedBy(userId);
        workflow = workflowRepository.save(workflow);

        log.info("Workflow {} rolled back to version {}", workflowId, versionNumber);
        return mapWorkflowToResponse(workflow);
    }

    @Transactional
    public WorkflowResponse copyWorkflow(UUID workflowId, String newName, String newDescription,
                                          UUID targetProjectId, UUID userId) {
        log.info("Copying workflow {} to new workflow: {}", workflowId, newName);

        Workflow original = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", workflowId));

        Workflow copy = Workflow.builder()
                .name(newName)
                .description(newDescription != null ? newDescription : original.getDescription())
                .projectId(targetProjectId)
                .isDefault(false)
                .isDraft(false)
                .isActive(true)
                .isSystem(false)
                .type(original.getType())
                .statusCategoryMapping(original.getStatusCategoryMapping())
                .createdBy(userId)
                .originalWorkflowId(original.getId())
                .build();

        copy = workflowRepository.save(copy);

        List<WorkflowStatus> originalStatuses = workflowStatusRepository.findByWorkflowIdOrderBySequenceAsc(workflowId);
        List<WorkflowStatus> newStatuses = new ArrayList<>();
        Map<UUID, UUID> statusIdMapping = new HashMap<>();

        for (WorkflowStatus originalStatus : originalStatuses) {
            WorkflowStatus newStatus = WorkflowStatus.builder()
                    .workflowId(copy.getId())
                    .statusId(originalStatus.getStatusId())
                    .sequence(originalStatus.getSequence())
                    .build();
            newStatus = workflowStatusRepository.save(newStatus);
            newStatuses.add(newStatus);
            statusIdMapping.put(originalStatus.getId(), newStatus.getId());
        }

        List<WorkflowTransition> originalTransitions = workflowTransitionRepository.findByWorkflowId(workflowId);
        for (WorkflowTransition originalTransition : originalTransitions) {
            WorkflowTransition newTransition = WorkflowTransition.builder()
                    .workflowId(copy.getId())
                    .name(originalTransition.getName())
                    .description(originalTransition.getDescription())
                    .fromStatusId(originalTransition.getFromStatusId())
                    .toStatusId(originalTransition.getToStatusId())
                    .displayOrder(originalTransition.getDisplayOrder())
                    .type(originalTransition.getType())
                    .icon(originalTransition.getIcon())
                    .conditionConditions(originalTransition.getConditionConditions())
                    .conditionOperator(originalTransition.getConditionOperator())
                    .validatorValidators(originalTransition.getValidatorValidators())
                    .postFunctionFunctions(originalTransition.getPostFunctionFunctions())
                    .screenId(originalTransition.getScreenId())
                    .permissionCheck(originalTransition.getPermissionCheck())
                    .userGroupIds(originalTransition.getUserGroupIds())
                    .createdBy(userId)
                    .build();
            newTransition = workflowTransitionRepository.save(newTransition);

            copyConditions(originalTransition.getId(), newTransition.getId());
            copyValidators(originalTransition.getId(), newTransition.getId());
            copyPostFunctions(originalTransition.getId(), newTransition.getId());
            copyProperties(originalTransition.getId(), newTransition.getId());
        }

        log.info("Workflow copied to: {}", copy.getId());
        return mapWorkflowToResponse(copy);
    }

    private Map<String, Object> buildDraftData(Workflow workflow) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", workflow.getName());
        data.put("description", workflow.getDescription());
        data.put("statusCategoryMapping", workflow.getStatusCategoryMapping());
        data.put("type", workflow.getType());
        return data;
    }

    private int getNextDraftVersion(UUID workflowId) {
        List<WorkflowDraft> drafts = workflowDraftRepository.findByWorkflowId(workflowId);
        return drafts.stream()
                .mapToInt(d -> d.getParentVersion() != null ? d.getParentVersion() : 0)
                .max()
                .orElse(0) + 1;
    }

    private int getNextWorkflowVersion(UUID workflowId) {
        List<WorkflowVersion> versions = workflowVersionRepository.findByWorkflowId(workflowId);
        return versions.stream()
                .mapToInt(WorkflowVersion::getVersionNumber)
                .max()
                .orElse(0) + 1;
    }

    private void createVersionSnapshot(Workflow workflow, int versionNumber,
                                       String changeDescription, UUID userId) {
        Map<String, Object> snapshot = buildDraftData(workflow);

        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshot);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize workflow snapshot", e);
        }

        WorkflowVersion version = WorkflowVersion.builder()
                .workflow(workflow)
                .versionNumber(versionNumber)
                .workflowSnapshot(snapshotJson)
                .createdBy(userId)
                .changeDescription(changeDescription)
                .changeType("UPDATE")
                .build();

        workflowVersionRepository.save(version);
    }

    @SuppressWarnings("unchecked")
    private void applyDraftChanges(Workflow workflow, String draftData) {
        try {
            Map<String, Object> data = objectMapper.readValue(draftData, Map.class);
            if (data.containsKey("name")) {
                workflow.setName((String) data.get("name"));
            }
            if (data.containsKey("description")) {
                workflow.setDescription((String) data.get("description"));
            }
            if (data.containsKey("statusCategoryMapping")) {
                workflow.setStatusCategoryMapping((String) data.get("statusCategoryMapping"));
            }
        } catch (Exception e) {
            log.error("Failed to apply draft changes: {}", e.getMessage());
        }
    }

    private void copyConditions(UUID fromTransitionId, UUID toTransitionId) {
        List<WorkflowCondition> conditions = workflowConditionRepository.findByTransitionIdOrderBySequenceAsc(fromTransitionId);
        for (WorkflowCondition condition : conditions) {
            WorkflowCondition copy = WorkflowCondition.builder()
                    .transitionId(toTransitionId)
                    .conditionType(condition.getConditionType())
                    .fieldName(condition.getFieldName())
                    .operator(condition.getOperator())
                    .value(condition.getValue())
                    .conditionData(condition.getConditionData())
                    .negate(condition.getNegate())
                    .sequence(condition.getSequence())
                    .build();
            workflowConditionRepository.save(copy);
        }
    }

    private void copyValidators(UUID fromTransitionId, UUID toTransitionId) {
        List<WorkflowValidator> validators = workflowValidatorRepository.findByTransitionIdOrderBySequenceAsc(fromTransitionId);
        for (WorkflowValidator validator : validators) {
            WorkflowValidator copy = WorkflowValidator.builder()
                    .transitionId(toTransitionId)
                    .validatorType(validator.getValidatorType())
                    .fieldName(validator.getFieldName())
                    .validatorData(validator.getValidatorData())
                    .errorMessage(validator.getErrorMessage())
                    .sequence(validator.getSequence())
                    .continueOnError(validator.getContinueOnError())
                    .build();
            workflowValidatorRepository.save(copy);
        }
    }

    private void copyPostFunctions(UUID fromTransitionId, UUID toTransitionId) {
        List<WorkflowPostFunction> functions = workflowPostFunctionRepository.findByTransitionIdOrderBySequenceAsc(fromTransitionId);
        for (WorkflowPostFunction function : functions) {
            WorkflowPostFunction copy = WorkflowPostFunction.builder()
                    .transitionId(toTransitionId)
                    .functionType(function.getFunctionType())
                    .functionData(function.getFunctionData())
                    .sequence(function.getSequence())
                    .async(function.getAsync())
                    .failOnError(function.getFailOnError())
                    .build();
            workflowPostFunctionRepository.save(copy);
        }
    }

    private void copyProperties(UUID fromTransitionId, UUID toTransitionId) {
        List<WorkflowTransitionProperty> properties = workflowTransitionPropertyRepository.findByTransitionId(fromTransitionId);
        for (WorkflowTransitionProperty property : properties) {
            WorkflowTransitionProperty copy = WorkflowTransitionProperty.builder()
                    .transitionId(toTransitionId)
                    .propertyKey(property.getPropertyKey())
                    .propertyValue(property.getPropertyValue())
                    .propertyType(property.getPropertyType())
                    .isSystem(property.getIsSystem())
                    .build();
            workflowTransitionPropertyRepository.save(copy);
        }
    }

    private WorkflowDraftResponse mapToResponse(WorkflowDraft draft, Workflow workflow) {
        return WorkflowDraftResponse.builder()
                .id(draft.getId())
                .workflowId(draft.getWorkflowId())
                .workflowName(workflow != null ? workflow.getName() : null)
                .name(draft.getName())
                .description(draft.getDescription())
                .draftData(draft.getDraftData())
                .parentVersion(draft.getParentVersion())
                .createdBy(draft.getCreatedBy())
                .createdAt(draft.getCreatedAt())
                .updatedAt(draft.getUpdatedAt())
                .isDraftOfPublished(draft.getIsDraftOfPublished())
                .draftStatus(draft.getDraftStatus())
                .build();
    }

    private WorkflowResponse mapWorkflowToResponse(Workflow workflow) {
        List<WorkflowStatus> statuses = workflowStatusRepository.findByWorkflowIdOrderBySequenceAsc(workflow.getId());
        List<UUID> statusIds = statuses.stream()
                .map(WorkflowStatus::getStatusId)
                .collect(Collectors.toList());

        return WorkflowResponse.builder()
                .id(workflow.getId())
                .projectId(workflow.getProjectId())
                .name(workflow.getName())
                .description(workflow.getDescription())
                .isDefault(workflow.getIsDefault())
                .isDraft(workflow.getIsDraft())
                .isActive(workflow.getIsActive())
                .isSystem(workflow.getIsSystem())
                .isLocked(workflow.getIsLocked())
                .lockedBy(workflow.getLockedBy())
                .lockedAt(workflow.getLockedAt())
                .publishedAt(workflow.getPublishedAt())
                .type(workflow.getType())
                .statusIds(statusIds)
                .statusCount(statuses.size())
                .createdAt(workflow.getCreatedAt())
                .updatedAt(workflow.getUpdatedAt())
                .createdBy(workflow.getCreatedBy())
                .updatedBy(workflow.getUpdatedBy())
                .version(workflow.getVersion())
                .build();
    }

    private WorkflowVersionResponse mapVersionToResponse(WorkflowVersion version) {
        return WorkflowVersionResponse.builder()
                .id(version.getId())
                .workflowId(version.getWorkflow().getId())
                .versionNumber(version.getVersionNumber())
                .workflowSnapshot(version.getWorkflowSnapshot())
                .createdAt(version.getCreatedAt())
                .createdBy(version.getCreatedBy())
                .changeDescription(version.getChangeDescription())
                .changeType(version.getChangeType())
                .build();
    }
}