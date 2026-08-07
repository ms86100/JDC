package com.avionics_systems.workflow.service;

import com.avionics_systems.workflow.dto.WorkflowMigrationResponse;
import com.avionics_systems.workflow.engine.WorkflowIntegrationClient;
import com.avionics_systems.workflow.engine.plugin.WorkflowPluginRegistry;
import com.avionics_systems.workflow.entity.*;
import com.avionics_systems.workflow.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowAdministrationService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStatusRepository workflowStatusRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowSchemeRepository workflowSchemeRepository;
    private final WorkflowSchemeMappingRepository workflowSchemeMappingRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final WorkflowScreenRepository workflowScreenRepository;
    private final WorkflowScreenTabRepository workflowScreenTabRepository;
    private final WorkflowScreenFieldRepository workflowScreenFieldRepository;
    private final WorkflowAuditLogRepository workflowAuditLogRepository;
    private final WorkflowPluginRegistry pluginRegistry;
    private final WorkflowConditionRepository workflowConditionRepository2;
    private final WorkflowValidatorRepository workflowValidatorRepository;
    private final WorkflowPostFunctionRepository workflowPostFunctionRepository;
    private final WorkflowTransitionHistoryRepository workflowTransitionHistoryRepository;
    private final WorkflowMigrationService workflowMigrationService;
    private final WorkflowIntegrationClient workflowIntegrationClient;
    private final ObjectMapper objectMapper;

    @Value("${app.workflow.status-category.color-todo:#6C757D}")
    private String statusCategoryColorTodo;

    @Value("${app.workflow.status-category.color-in-progress:#0066FF}")
    private String statusCategoryColorInProgress;

    @Value("${app.workflow.status-category.color-done:#28A745}")
    private String statusCategoryColorDone;

    // ==================== WORKFLOW CRUD ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listWorkflows(String status, String name) {
        List<Workflow> workflows = workflowRepository.findAll();
        if (status != null) {
            if ("active".equalsIgnoreCase(status)) {
                workflows = workflows.stream().filter(w -> Boolean.TRUE.equals(w.getIsActive())).collect(Collectors.toList());
            } else if ("draft".equalsIgnoreCase(status)) {
                workflows = workflows.stream().filter(w -> Boolean.TRUE.equals(w.getIsDraft())).collect(Collectors.toList());
            } else if ("inactive".equalsIgnoreCase(status)) {
                workflows = workflows.stream().filter(w -> !Boolean.TRUE.equals(w.getIsActive()) && !Boolean.TRUE.equals(w.getIsDraft())).collect(Collectors.toList());
            }
        }
        if (name != null && !name.isEmpty()) {
            workflows = workflows.stream().filter(w -> w.getName().toLowerCase().contains(name.toLowerCase())).collect(Collectors.toList());
        }
        return workflows.stream().map(this::workflowToMap).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getWorkflow(UUID workflowId) {
        return workflowRepository.findById(workflowId).map(this::workflowToMap);
    }

    @Transactional
    public Map<String, Object> createWorkflow(Map<String, Object> data) {
        String name = (String) data.get("name");
        if (workflowRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Workflow with name '" + name + "' already exists");
        }

        String statusMapping;
        try {
            statusMapping = objectMapper.writeValueAsString(getStatusCategoryMapping());
        } catch (Exception e) {
            statusMapping = "{}";
        }

        Workflow workflow = Workflow.builder()
                .name(name)
                .description((String) data.getOrDefault("description", ""))
                .isDefault(false)
                .isDraft(true)
                .isActive(false)
                .isSystem(false)
                .type((String) data.getOrDefault("type", "CUSTOM"))
                .projectId(data.get("projectId") != null ? UUID.fromString((String) data.get("projectId")) : null)
                .statusCategoryMapping(statusMapping)
                .build();

        workflow = workflowRepository.save(workflow);

        // Create version 1
        createVersionSnapshot(workflow, "Workflow created", "CREATE");

        logAudit("CREATE", "WORKFLOW", workflow.getId(), workflow.getName(), "Workflow created");
        log.info("Created workflow: {} ({})", workflow.getName(), workflow.getId());

        return workflowToMap(workflow);
    }

    @Transactional
    public Map<String, Object> updateWorkflow(UUID workflowId, Map<String, Object> updates) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        if (Boolean.TRUE.equals(workflow.getIsSystem())) {
            throw new IllegalArgumentException("System workflows cannot be modified");
        }

        if (updates.containsKey("name")) {
            workflow.setName((String) updates.get("name"));
        }
        if (updates.containsKey("description")) {
            workflow.setDescription((String) updates.get("description"));
        }
        if (updates.containsKey("statusCategoryMapping")) {
            try {
                workflow.setStatusCategoryMapping(objectMapper.writeValueAsString(updates.get("statusCategoryMapping")));
            } catch (Exception e) {
                log.error("Failed to serialize status category mapping", e);
            }
        }

        workflow = workflowRepository.save(workflow);
        logAudit("UPDATE", "WORKFLOW", workflow.getId(), workflow.getName(), "Workflow updated");
        return workflowToMap(workflow);
    }

    @Transactional
    public void deleteWorkflow(UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        if (Boolean.TRUE.equals(workflow.getIsSystem())) {
            throw new IllegalArgumentException("System workflows cannot be deleted");
        }

        // Check if workflow is used
        List<WorkflowSchemeMapping> mappings = workflowSchemeMappingRepository.findByWorkflowId(workflowId);
        if (!mappings.isEmpty()) {
            throw new IllegalStateException("Workflow is used by " + mappings.size() + " scheme(s)");
        }

        // H6: Cascade delete transitions and their normalized dependencies
        List<WorkflowTransition> transitions = workflowTransitionRepository.findByWorkflowId(workflowId);
        for (WorkflowTransition t : transitions) {
            workflowConditionRepository2.deleteByTransitionId(t.getId());
            workflowValidatorRepository.deleteByTransitionId(t.getId());
            workflowPostFunctionRepository.deleteByTransitionId(t.getId());
        }
        workflowTransitionRepository.deleteAll(transitions);

        // Delete statuses
        workflowStatusRepository.deleteAll(
                workflowStatusRepository.findByWorkflowIdOrderBySequenceAsc(workflowId));

        // Delete versions
        workflowVersionRepository.findByWorkflowIdOrderByVersionNumberDesc(workflowId)
                .forEach(workflowVersionRepository::delete);

        workflowRepository.delete(workflow);
        logAudit("DELETE", "WORKFLOW", workflowId, workflow.getName(), "Workflow deleted");
        log.info("Deleted workflow: {} ({})", workflow.getName(), workflowId);
    }

    @Transactional
    public Map<String, Object> cloneWorkflow(UUID workflowId, String newName) {
        Workflow original = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        if (workflowRepository.findByName(newName).isPresent()) {
            throw new IllegalArgumentException("Workflow with name '" + newName + "' already exists");
        }

        Workflow clone = Workflow.builder()
                .name(newName)
                .description(original.getDescription())
                .projectId(original.getProjectId())
                .isDefault(false)
                .isDraft(true)
                .isActive(false)
                .statusCategoryMapping(original.getStatusCategoryMapping())
                .build();

        clone = workflowRepository.save(clone);

        // Clone statuses
        cloneWorkflowStatuses(original.getId(), clone.getId());

        // Clone transitions
        cloneWorkflowTransitions(original.getId(), clone.getId());

        createVersionSnapshot(clone, "Cloned from " + original.getName(), "CREATE");
        logAudit("CLONE", "WORKFLOW", clone.getId(), clone.getName(), "Cloned from " + original.getName());

        return workflowToMap(clone);
    }

    @Transactional
    public Map<String, Object> publishWorkflow(UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        // Save current state as new version
        createVersionSnapshot(workflow, "Published", "PUBLISH");

        workflow.setIsDraft(false);
        workflow.setIsActive(true);
        workflow.setPublishedAt(LocalDateTime.now());
        workflow = workflowRepository.save(workflow);

        logAudit("PUBLISH", "WORKFLOW", workflow.getId(), workflow.getName(), "Workflow published");
        return workflowToMap(workflow);
    }

    @Transactional
    public Map<String, Object> createDraft(UUID workflowId) {
        Workflow published = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        String draftName = published.getName() + " (Draft)";

        // Check if draft already exists
        Optional<Workflow> existingDraft = workflowRepository.findAll().stream()
                .filter(w -> w.getDraftOfWorkflowId() != null && w.getDraftOfWorkflowId().equals(workflowId))
                .filter(w -> Boolean.TRUE.equals(w.getIsDraft()))
                .findFirst();

        if (existingDraft.isPresent()) {
            return workflowToMap(existingDraft.get());
        }

        Workflow draft = Workflow.builder()
                .name(draftName)
                .description(published.getDescription())
                .projectId(published.getProjectId())
                .isDefault(false)
                .isDraft(true)
                .isActive(false)
                .draftOfWorkflowId(workflowId)
                .statusCategoryMapping(published.getStatusCategoryMapping())
                .build();

        draft = workflowRepository.save(draft);

        // Clone statuses and transitions
        cloneWorkflowStatuses(workflowId, draft.getId());
        cloneWorkflowTransitions(workflowId, draft.getId());

        logAudit("CREATE_DRAFT", "WORKFLOW", draft.getId(), draft.getName(), "Draft created from " + published.getName());
        return workflowToMap(draft);
    }

    // ==================== STATUS MANAGEMENT ====================

    @Transactional
    public Map<String, Object> addStatus(UUID workflowId, Map<String, Object> statusData) {
        if (!workflowRepository.existsById(workflowId)) {
            throw new IllegalArgumentException("Workflow not found: " + workflowId);
        }

        UUID statusId = UUID.fromString((String) statusData.get("statusId"));

        // Check for duplicate
        if (workflowStatusRepository.existsByWorkflowIdAndStatusId(workflowId, statusId)) {
            throw new IllegalArgumentException("Status already exists in workflow");
        }

        WorkflowStatus status = WorkflowStatus.builder()
                .workflowId(workflowId)
                .statusId(statusId)
                .sequence((Integer) statusData.getOrDefault("sequence", 0))
                .build();

        status = workflowStatusRepository.save(status);

        logAudit("ADD_STATUS", "WORKFLOW_STATUS", status.getId(), statusId.toString(),
                "Status added to workflow " + workflowId);

        return statusToMap(status);
    }

    @Transactional
    public Map<String, Object> updateStatus(UUID workflowId, UUID statusId, Map<String, Object> updates) {
        WorkflowStatus status = workflowStatusRepository.findById(statusId)
                .orElseThrow(() -> new IllegalArgumentException("Status not found: " + statusId));

        if (!status.getWorkflowId().equals(workflowId)) {
            throw new IllegalArgumentException("Status does not belong to this workflow");
        }

        if (updates.containsKey("sequence")) {
            status.setSequence((Integer) updates.get("sequence"));
        }

        status = workflowStatusRepository.save(status);

        logAudit("UPDATE_STATUS", "WORKFLOW_STATUS", statusId, statusId.toString(), "Status updated");
        return statusToMap(status);
    }

    @Transactional
    public void removeStatus(UUID workflowId, UUID statusId) {
        List<WorkflowTransition> transitions = workflowTransitionRepository.findByWorkflowId(workflowId);

        // Check if status is used in transitions
        boolean inTransition = transitions.stream()
                .anyMatch(t -> t.getFromStatusId().equals(statusId) || t.getToStatusId().equals(statusId));

        if (inTransition) {
            throw new IllegalStateException("Cannot remove status that is used in transitions");
        }

        workflowStatusRepository.deleteById(statusId);

        logAudit("REMOVE_STATUS", "WORKFLOW_STATUS", statusId, statusId.toString(), "Status removed from workflow");
    }

    // ==================== TRANSITION MANAGEMENT ====================

    @Transactional
    public Map<String, Object> addTransition(UUID workflowId, Map<String, Object> transitionData) {
        if (!workflowRepository.existsById(workflowId)) {
            throw new IllegalArgumentException("Workflow not found: " + workflowId);
        }

        UUID fromStatusId = UUID.fromString((String) transitionData.get("fromStatusId"));
        UUID toStatusId = UUID.fromString((String) transitionData.get("toStatusId"));

        // Verify statuses exist in workflow
        if (!workflowStatusRepository.existsByWorkflowIdAndStatusId(workflowId, fromStatusId)) {
            throw new IllegalArgumentException("From status not found in workflow");
        }
        if (!workflowStatusRepository.existsByWorkflowIdAndStatusId(workflowId, toStatusId)) {
            throw new IllegalArgumentException("To status not found in workflow");
        }

        // Check for duplicate transition
        Optional<WorkflowTransition> existing = workflowTransitionRepository
                .findByWorkflowIdAndFromStatusIdAndToStatusId(workflowId, fromStatusId, toStatusId);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Transition already exists between these statuses");
        }

        WorkflowTransition transition = WorkflowTransition.builder()
                .workflowId(workflowId)
                .fromStatusId(fromStatusId)
                .toStatusId(toStatusId)
                .name((String) transitionData.getOrDefault("name", "Transition"))
                .description((String) transitionData.getOrDefault("description", ""))
                .displayOrder((Integer) transitionData.getOrDefault("displayOrder", 1))
                .type((String) transitionData.getOrDefault("type", "MANUAL"))
                .build();

        transition = workflowTransitionRepository.save(transition);

        logAudit("ADD_TRANSITION", "WORKFLOW_TRANSITION", transition.getId(), transition.getName(),
                "Transition added: " + fromStatusId + " -> " + toStatusId);

        return transitionToMap(transition);
    }

    @Transactional
    public Map<String, Object> updateTransition(UUID workflowId, UUID transitionId, Map<String, Object> updates) {
        WorkflowTransition transition = workflowTransitionRepository.findById(transitionId)
                .orElseThrow(() -> new IllegalArgumentException("Transition not found: " + transitionId));

        if (!transition.getWorkflowId().equals(workflowId)) {
            throw new IllegalArgumentException("Transition does not belong to this workflow");
        }

        if (updates.containsKey("name")) transition.setName((String) updates.get("name"));
        if (updates.containsKey("description")) transition.setDescription((String) updates.get("description"));
        if (updates.containsKey("displayOrder")) transition.setDisplayOrder((Integer) updates.get("displayOrder"));
        if (updates.containsKey("type")) transition.setType((String) updates.get("type"));
        if (updates.containsKey("conditions")) {
            try {
                transition.setConditions(objectMapper.writeValueAsString(updates.get("conditions")));
            } catch (Exception e) {
                log.error("Failed to serialize conditions", e);
            }
        }
        if (updates.containsKey("validators")) {
            try {
                transition.setValidators(objectMapper.writeValueAsString(updates.get("validators")));
            } catch (Exception e) {
                log.error("Failed to serialize validators", e);
            }
        }
        if (updates.containsKey("postFunctions")) {
            try {
                transition.setPostFunctions(objectMapper.writeValueAsString(updates.get("postFunctions")));
            } catch (Exception e) {
                log.error("Failed to serialize post functions", e);
            }
        }
        if (updates.containsKey("screenId")) {
            transition.setScreenId(UUID.fromString((String) updates.get("screenId")));
        }

        transition = workflowTransitionRepository.save(transition);
        logAudit("UPDATE_TRANSITION", "WORKFLOW_TRANSITION", transitionId, transition.getName(), "Transition updated");
        return transitionToMap(transition);
    }

    @Transactional
    public void deleteTransition(UUID workflowId, UUID transitionId) {
        WorkflowTransition transition = workflowTransitionRepository.findById(transitionId)
                .orElseThrow(() -> new IllegalArgumentException("Transition not found: " + transitionId));

        if (!transition.getWorkflowId().equals(workflowId)) {
            throw new IllegalArgumentException("Transition does not belong to this workflow");
        }

        workflowTransitionRepository.delete(transition);
        logAudit("DELETE_TRANSITION", "WORKFLOW_TRANSITION", transitionId, transition.getName(), "Transition deleted");
    }

    @Transactional
    public void reorderStatuses(UUID workflowId, List<UUID> statusIds) {
        int[] order = {0};
        for (UUID statusId : statusIds) {
            int currentOrder = order[0];
            workflowStatusRepository.findById(statusId).ifPresent(status -> {
                if (status.getWorkflowId().equals(workflowId)) {
                    status.setSequence(currentOrder);
                    workflowStatusRepository.save(status);
                }
            });
            order[0]++;
        }
        logAudit("REORDER_STATUSES", "WORKFLOW", workflowId, null, "Statuses reordered");
    }

    @Transactional
    public void reorderTransitions(UUID workflowId, UUID fromStatusId, List<UUID> toStatusIds) {
        int[] order = {0};
        for (UUID toStatusId : toStatusIds) {
            int currentOrder = order[0];
            workflowTransitionRepository.findByWorkflowIdAndFromStatusIdAndToStatusId(workflowId, fromStatusId, toStatusId)
                    .ifPresent(transition -> {
                        transition.setDisplayOrder(currentOrder);
                        workflowTransitionRepository.save(transition);
                    });
            order[0]++;
        }
        logAudit("REORDER_TRANSITIONS", "WORKFLOW", workflowId, null, "Transitions reordered from " + fromStatusId);
    }

    // ==================== CONDITION MANAGEMENT ====================

    @Transactional
    public Map<String, Object> addCondition(UUID transitionId, Map<String, Object> conditionData) {
        WorkflowTransition transition = workflowTransitionRepository.findById(transitionId)
                .orElseThrow(() -> new IllegalArgumentException("Transition not found: " + transitionId));

        List<Map<String, Object>> conditions = new ArrayList<>();
        try {
            if (transition.getConditions() != null) {
                conditions = objectMapper.readValue(transition.getConditions(), List.class);
            }
        } catch (Exception e) {
            log.error("Failed to parse existing conditions", e);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> condition = (Map<String, Object>) conditionData;
        UUID generatedId = UUID.randomUUID();
        condition.put("id", generatedId.toString());
        conditions.add(condition);

        try {
            transition.setConditions(objectMapper.writeValueAsString(conditions));
        } catch (Exception e) {
            log.error("Failed to serialize conditions", e);
        }

        workflowTransitionRepository.save(transition);

        // C9: Create normalized entity for runtime engine sync
        try {
            String condType = (String) conditionData.getOrDefault("type",
                    (String) conditionData.getOrDefault("conditionType", "CUSTOM"));
            WorkflowCondition normalizedCondition = WorkflowCondition.builder()
                    .id(generatedId)
                    .transitionId(transitionId)
                    .conditionType(condType)
                    .fieldName((String) conditionData.get("fieldName"))
                    .operator((String) conditionData.get("operator"))
                    .value(conditionData.get("value") != null ? String.valueOf(conditionData.get("value")) : null)
                    .conditionData(serializeToJson(conditionData))
                    .negate(Boolean.TRUE.equals(conditionData.get("negate")))
                    .sequence(conditions.size() - 1)
                    .build();
            workflowConditionRepository2.save(normalizedCondition);
        } catch (Exception e) {
            log.warn("Failed to create normalized condition entity, JSON blob still saved", e);
        }

        logAudit("ADD_CONDITION", "WORKFLOW_TRANSITION", transitionId, transition.getName(), "Condition added");
        return condition;
    }

    @Transactional
    public void removeCondition(UUID transitionId, UUID conditionId) {
        // C5: Resolve transitionId when controller passes null
        UUID resolvedTransitionId = transitionId;
        if (resolvedTransitionId == null) {
            WorkflowCondition conditionEntity = workflowConditionRepository2.findById(conditionId).orElse(null);
            if (conditionEntity != null) {
                resolvedTransitionId = conditionEntity.getTransitionId();
                workflowConditionRepository2.delete(conditionEntity);
            }
        } else {
            // Also clean up normalized entity if it exists
            workflowConditionRepository2.findById(conditionId).ifPresent(workflowConditionRepository2::delete);
        }
        if (resolvedTransitionId == null) {
            log.warn("Cannot find transition for condition {}", conditionId);
            return;
        }

        final UUID finalTransitionId = resolvedTransitionId;
        WorkflowTransition transition = workflowTransitionRepository.findById(finalTransitionId)
                .orElseThrow(() -> new IllegalArgumentException("Transition not found: " + finalTransitionId));

        List<Map<String, Object>> conditions = new ArrayList<>();
        try {
            if (transition.getConditions() != null) {
                conditions = objectMapper.readValue(transition.getConditions(), List.class);
            }
        } catch (Exception e) {
            log.error("Failed to parse conditions", e);
        }

        conditions.removeIf(c -> conditionId.toString().equals(c.get("id")));
        try {
            transition.setConditions(objectMapper.writeValueAsString(conditions));
        } catch (Exception e) {
            log.error("Failed to serialize conditions", e);
        }

        workflowTransitionRepository.save(transition);

        logAudit("REMOVE_CONDITION", "WORKFLOW_TRANSITION", resolvedTransitionId, transition.getName(), "Condition removed");
    }

    // ==================== VALIDATOR MANAGEMENT ====================

    @Transactional
    public Map<String, Object> addValidator(UUID transitionId, Map<String, Object> validatorData) {
        WorkflowTransition transition = workflowTransitionRepository.findById(transitionId)
                .orElseThrow(() -> new IllegalArgumentException("Transition not found: " + transitionId));

        List<Map<String, Object>> validators = new ArrayList<>();
        try {
            if (transition.getValidators() != null) {
                validators = objectMapper.readValue(transition.getValidators(), List.class);
            }
        } catch (Exception e) {
            log.error("Failed to parse existing validators", e);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> validator = (Map<String, Object>) validatorData;
        UUID generatedId = UUID.randomUUID();
        validator.put("id", generatedId.toString());
        validators.add(validator);

        try {
            transition.setValidators(objectMapper.writeValueAsString(validators));
        } catch (Exception e) {
            log.error("Failed to serialize validators", e);
        }

        workflowTransitionRepository.save(transition);

        // C9: Create normalized entity for runtime engine sync
        try {
            String valType = (String) validatorData.getOrDefault("type",
                    (String) validatorData.getOrDefault("validatorType", "CUSTOM"));
            WorkflowValidator normalizedValidator = WorkflowValidator.builder()
                    .id(generatedId)
                    .transitionId(transitionId)
                    .validatorType(valType)
                    .fieldName((String) validatorData.get("fieldName"))
                    .validatorData(serializeToJson(validatorData))
                    .errorMessage((String) validatorData.get("errorMessage"))
                    .sequence(validators.size() - 1)
                    .build();
            workflowValidatorRepository.save(normalizedValidator);
        } catch (Exception e) {
            log.warn("Failed to create normalized validator entity, JSON blob still saved", e);
        }

        logAudit("ADD_VALIDATOR", "WORKFLOW_TRANSITION", transitionId, transition.getName(), "Validator added");
        return validator;
    }

    @Transactional
    public void removeValidator(UUID transitionId, UUID validatorId) {
        // C5: Resolve transitionId when controller passes null
        UUID resolvedTransitionId = transitionId;
        if (resolvedTransitionId == null) {
            WorkflowValidator validatorEntity = workflowValidatorRepository.findById(validatorId).orElse(null);
            if (validatorEntity != null) {
                resolvedTransitionId = validatorEntity.getTransitionId();
                workflowValidatorRepository.delete(validatorEntity);
            }
        } else {
            // Also clean up normalized entity if it exists
            workflowValidatorRepository.findById(validatorId).ifPresent(workflowValidatorRepository::delete);
        }
        if (resolvedTransitionId == null) {
            log.warn("Cannot find transition for validator {}", validatorId);
            return;
        }

        final UUID finalTransitionId = resolvedTransitionId;
        WorkflowTransition transition = workflowTransitionRepository.findById(finalTransitionId)
                .orElseThrow(() -> new IllegalArgumentException("Transition not found: " + finalTransitionId));

        List<Map<String, Object>> validators = new ArrayList<>();
        try {
            if (transition.getValidators() != null) {
                validators = objectMapper.readValue(transition.getValidators(), List.class);
            }
        } catch (Exception e) {
            log.error("Failed to parse validators", e);
        }

        validators.removeIf(v -> validatorId.toString().equals(v.get("id")));
        try {
            transition.setValidators(objectMapper.writeValueAsString(validators));
        } catch (Exception e) {
            log.error("Failed to serialize validators", e);
        }

        workflowTransitionRepository.save(transition);

        logAudit("REMOVE_VALIDATOR", "WORKFLOW_TRANSITION", resolvedTransitionId, transition.getName(), "Validator removed");
    }

    // ==================== POST FUNCTION MANAGEMENT ====================

    @Transactional
    public Map<String, Object> addPostFunction(UUID transitionId, Map<String, Object> functionData) {
        WorkflowTransition transition = workflowTransitionRepository.findById(transitionId)
                .orElseThrow(() -> new IllegalArgumentException("Transition not found: " + transitionId));

        List<Map<String, Object>> functions = new ArrayList<>();
        try {
            if (transition.getPostFunctions() != null) {
                functions = objectMapper.readValue(transition.getPostFunctions(), List.class);
            }
        } catch (Exception e) {
            log.error("Failed to parse existing post functions", e);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> function = (Map<String, Object>) functionData;
        UUID generatedId = UUID.randomUUID();
        function.put("id", generatedId.toString());
        functions.add(function);

        try {
            transition.setPostFunctions(objectMapper.writeValueAsString(functions));
        } catch (Exception e) {
            log.error("Failed to serialize post functions", e);
        }

        workflowTransitionRepository.save(transition);

        // C9: Create normalized entity for runtime engine sync
        try {
            String funcType = (String) functionData.getOrDefault("type",
                    (String) functionData.getOrDefault("functionType", "CUSTOM"));
            WorkflowPostFunction normalizedPostFunction = WorkflowPostFunction.builder()
                    .id(generatedId)
                    .transitionId(transitionId)
                    .functionType(funcType)
                    .functionData(serializeToJson(functionData))
                    .sequence(functions.size() - 1)
                    .enabled(true)
                    .build();
            workflowPostFunctionRepository.save(normalizedPostFunction);
        } catch (Exception e) {
            log.warn("Failed to create normalized post-function entity, JSON blob still saved", e);
        }

        logAudit("ADD_POST_FUNCTION", "WORKFLOW_TRANSITION", transitionId, transition.getName(), "Post function added");
        return function;
    }

    @Transactional
    public void removePostFunction(UUID transitionId, UUID functionId) {
        // C5: Resolve transitionId when controller passes null
        UUID resolvedTransitionId = transitionId;
        if (resolvedTransitionId == null) {
            WorkflowPostFunction postFunctionEntity = workflowPostFunctionRepository.findById(functionId).orElse(null);
            if (postFunctionEntity != null) {
                resolvedTransitionId = postFunctionEntity.getTransitionId();
                workflowPostFunctionRepository.delete(postFunctionEntity);
            }
        } else {
            // Also clean up normalized entity if it exists
            workflowPostFunctionRepository.findById(functionId).ifPresent(workflowPostFunctionRepository::delete);
        }
        if (resolvedTransitionId == null) {
            log.warn("Cannot find transition for post function {}", functionId);
            return;
        }

        final UUID finalTransitionId = resolvedTransitionId;
        WorkflowTransition transition = workflowTransitionRepository.findById(finalTransitionId)
                .orElseThrow(() -> new IllegalArgumentException("Transition not found: " + finalTransitionId));

        List<Map<String, Object>> functions = new ArrayList<>();
        try {
            if (transition.getPostFunctions() != null) {
                functions = objectMapper.readValue(transition.getPostFunctions(), List.class);
            }
        } catch (Exception e) {
            log.error("Failed to parse post functions", e);
        }

        functions.removeIf(f -> functionId.toString().equals(f.get("id")));
        try {
            transition.setPostFunctions(objectMapper.writeValueAsString(functions));
        } catch (Exception e) {
            log.error("Failed to serialize post functions", e);
        }

        workflowTransitionRepository.save(transition);

        logAudit("REMOVE_POST_FUNCTION", "WORKFLOW_TRANSITION", resolvedTransitionId, transition.getName(), "Post function removed");
    }

    // ==================== WORKFLOW SCHEME MANAGEMENT ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllSchemes() {
        return workflowSchemeRepository.findAll().stream()
                .map(this::schemeToMap)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getSchemeById(UUID schemeId) {
        return workflowSchemeRepository.findById(schemeId)
                .map(this::schemeToMap);
    }

    @Transactional
    public Map<String, Object> createScheme(Map<String, Object> data) {
        String name = (String) data.get("name");
        if (workflowSchemeRepository.existsByName(name)) {
            throw new IllegalArgumentException("Scheme with name '" + name + "' already exists");
        }

        WorkflowScheme scheme = WorkflowScheme.builder()
                .name(name)
                .description((String) data.getOrDefault("description", ""))
                .isDefault(false)
                .build();

        scheme = workflowSchemeRepository.save(scheme);
        logAudit("CREATE", "WORKFLOW_SCHEME", scheme.getId(), scheme.getName(), "Scheme created");

        return schemeToMap(scheme);
    }

    @Transactional
    public Map<String, Object> updateScheme(UUID schemeId, Map<String, Object> updates) {
        WorkflowScheme scheme = workflowSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + schemeId));

        if (updates.containsKey("name")) scheme.setName((String) updates.get("name"));
        if (updates.containsKey("description")) scheme.setDescription((String) updates.get("description"));

        scheme = workflowSchemeRepository.save(scheme);
        logAudit("UPDATE", "WORKFLOW_SCHEME", schemeId, scheme.getName(), "Scheme updated");
        return schemeToMap(scheme);
    }

    @Transactional
    public void deleteScheme(UUID schemeId) {
        WorkflowScheme scheme = workflowSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + schemeId));

        if (Boolean.TRUE.equals(scheme.getIsDefault())) {
            throw new IllegalArgumentException("Cannot delete default scheme");
        }

        workflowSchemeRepository.delete(scheme);
        logAudit("DELETE", "WORKFLOW_SCHEME", schemeId, scheme.getName(), "Scheme deleted");
    }

    @Transactional
    public Map<String, Object> assignWorkflowToScheme(UUID schemeId, Map<String, Object> mappingData) {
        UUID issueTypeId = UUID.fromString((String) mappingData.get("issueTypeId"));
        UUID workflowId = UUID.fromString((String) mappingData.get("workflowId"));
        return assignWorkflowToSchemeInternal(schemeId, issueTypeId, workflowId);
    }

    private Map<String, Object> assignWorkflowToSchemeInternal(UUID schemeId, UUID issueTypeId, UUID workflowId) {
        WorkflowScheme scheme = workflowSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + schemeId));

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        // Check if mapping exists
        Optional<WorkflowSchemeMapping> existing = workflowSchemeMappingRepository
                .findBySchemeIdAndIssueTypeId(schemeId, issueTypeId);

        WorkflowSchemeMapping mapping;
        if (existing.isPresent()) {
            mapping = existing.get();
            mapping.setWorkflow(workflow);
        } else {
            mapping = WorkflowSchemeMapping.builder()
                    .scheme(scheme)
                    .issueTypeId(issueTypeId)
                    .workflow(workflow)
                    .build();
            scheme.getMappings().add(mapping);
        }

        workflowSchemeMappingRepository.save(mapping);
        logAudit("ASSIGN_WORKFLOW", "WORKFLOW_SCHEME_MAPPING", mapping.getId(),
                "IssueType:" + issueTypeId, "Workflow assigned to scheme: " + workflow.getName());

        return schemeToMap(scheme);
    }

    // ==================== VERSION MANAGEMENT ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWorkflowVersions(UUID workflowId) {
        return workflowVersionRepository.findByWorkflowIdOrderByVersionNumberDesc(workflowId)
                .stream()
                .map(this::versionToMap)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getWorkflowVersion(UUID workflowId, Integer versionNumber) {
        return workflowVersionRepository.findByWorkflowIdAndVersionNumber(workflowId, versionNumber)
                .map(this::versionToMap);
    }

    @Transactional
    public Map<String, Object> revertToVersion(UUID workflowId, Integer versionNumber) {
        WorkflowVersion version = workflowVersionRepository.findByWorkflowIdAndVersionNumber(workflowId, versionNumber)
                .orElseThrow(() -> new IllegalArgumentException("Version not found"));

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));

        // Restore from snapshot
        workflow.setStatusCategoryMapping(version.getWorkflowSnapshot());

        // Create new version for revert
        createVersionSnapshot(workflow, "Reverted to version " + versionNumber, "REVERT");

        workflow = workflowRepository.save(workflow);
        logAudit("REVERT", "WORKFLOW", workflowId, workflow.getName(), "Reverted to version " + versionNumber);

        return workflowToMap(workflow);
    }

    // ==================== SCREEN MANAGEMENT ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllScreens() {
        return workflowScreenRepository.findAll().stream()
                .map(this::screenToMap)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createScreen(Map<String, Object> data) {
        String name = (String) data.get("name");
        String screenType = (String) data.get("screenType");

        if (workflowScreenRepository.existsByName(name)) {
            throw new IllegalArgumentException("Screen with name '" + name + "' already exists");
        }

        WorkflowScreen screen = WorkflowScreen.builder()
                .name(name)
                .description((String) data.getOrDefault("description", ""))
                .screenType(screenType != null ? screenType : "TRANSITION")
                .isSystem(false)
                .isDefault(false)
                .build();

        screen = workflowScreenRepository.save(screen);
        logAudit("CREATE", "WORKFLOW_SCREEN", screen.getId(), screen.getName(), "Screen created");

        return screenToMap(screen);
    }

    @Transactional
    public Map<String, Object> addScreenTab(UUID screenId, Map<String, Object> tabData) {
        WorkflowScreen screen = workflowScreenTabRepository.findByScreenIdOrderByOrderIndexAsc(screenId)
                .stream().findFirst().map(WorkflowScreenTab::getScreen)
                .or(() -> workflowScreenRepository.findById(screenId))
                .orElseThrow(() -> new IllegalArgumentException("Screen not found: " + screenId));

        int maxOrder = workflowScreenTabRepository.findByScreenIdOrderByOrderIndexAsc(screenId)
                .stream().mapToInt(WorkflowScreenTab::getOrderIndex).max().orElse(-1);

        WorkflowScreenTab tab = WorkflowScreenTab.builder()
                .screen(screen)
                .tabName((String) tabData.get("tabName"))
                .description((String) tabData.getOrDefault("description", ""))
                .orderIndex(maxOrder + 1)
                .build();

        tab = workflowScreenTabRepository.save(tab);
        logAudit("ADD_TAB", "WORKFLOW_SCREEN_TAB", tab.getId(), tab.getTabName(), "Tab added to screen");

        return tabToMap(tab);
    }

    @Transactional
    public Map<String, Object> configureScreenFields(UUID tabId, List<Map<String, Object>> fields) {
        WorkflowScreenTab tab = workflowScreenTabRepository.findById(tabId)
                .orElseThrow(() -> new IllegalArgumentException("Tab not found: " + tabId));

        // Clear existing fields
        workflowScreenFieldRepository.deleteByTabId(tabId);
        tab.getFields().clear();

        // Add new fields
        int order = 0;
        for (Map<String, Object> fieldData : fields) {
            WorkflowScreenField field = WorkflowScreenField.builder()
                    .tab(tab)
                    .fieldId((String) fieldData.get("fieldId"))
                    .fieldLabel((String) fieldData.getOrDefault("fieldLabel", (String) fieldData.get("fieldId")))
                    .fieldType((String) fieldData.getOrDefault("fieldType", "text"))
                    .required((Boolean) fieldData.getOrDefault("required", false))
                    .hidden((Boolean) fieldData.getOrDefault("hidden", false))
                    .readonly((Boolean) fieldData.getOrDefault("readonly", false))
                    .orderIndex(order++)
                    .build();

            tab.getFields().add(field);
        }

        workflowScreenTabRepository.save(tab);
        logAudit("CONFIGURE_FIELDS", "WORKFLOW_SCREEN_TAB", tabId, tab.getTabName(), "Fields configured: " + fields.size());

        return tabToMap(tab);
    }

    @Transactional
    public Map<String, Object> updateScreenTab(UUID tabId, Map<String, Object> updates) {
        WorkflowScreenTab tab = workflowScreenTabRepository.findById(tabId)
                .orElseThrow(() -> new IllegalArgumentException("Tab not found: " + tabId));

        if (updates.containsKey("tabName")) tab.setTabName((String) updates.get("tabName"));
        if (updates.containsKey("description")) tab.setDescription((String) updates.get("description"));
        if (updates.containsKey("orderIndex")) tab.setOrderIndex((Integer) updates.get("orderIndex"));

        tab = workflowScreenTabRepository.save(tab);
        logAudit("UPDATE_TAB", "WORKFLOW_SCREEN_TAB", tabId, tab.getTabName(), "Tab updated");

        return tabToMap(tab);
    }

    @Transactional
    public void deleteScreenTab(UUID tabId) {
        WorkflowScreenTab tab = workflowScreenTabRepository.findById(tabId)
                .orElseThrow(() -> new IllegalArgumentException("Tab not found: " + tabId));

        workflowScreenTabRepository.delete(tab);
        logAudit("DELETE_TAB", "WORKFLOW_SCREEN_TAB", tabId, tab.getTabName(), "Tab deleted");
    }

    @Transactional
    public Map<String, Object> updateScreenField(UUID fieldId, Map<String, Object> updates) {
        WorkflowScreenField field = workflowScreenFieldRepository.findById(fieldId)
                .orElseThrow(() -> new IllegalArgumentException("Field not found: " + fieldId));

        if (updates.containsKey("fieldLabel")) field.setFieldLabel((String) updates.get("fieldLabel"));
        if (updates.containsKey("fieldType")) field.setFieldType((String) updates.get("fieldType"));
        if (updates.containsKey("required")) field.setRequired((Boolean) updates.get("required"));
        if (updates.containsKey("hidden")) field.setHidden((Boolean) updates.get("hidden"));
        if (updates.containsKey("readonly")) field.setReadonly((Boolean) updates.get("readonly"));
        if (updates.containsKey("orderIndex")) field.setOrderIndex((Integer) updates.get("orderIndex"));

        field = workflowScreenFieldRepository.save(field);
        logAudit("UPDATE_FIELD", "WORKFLOW_SCREEN_FIELD", fieldId, field.getFieldId(), "Field updated");

        return fieldToMap(field);
    }

    @Transactional
    public void deleteScreenField(UUID fieldId) {
        WorkflowScreenField field = workflowScreenFieldRepository.findById(fieldId)
                .orElseThrow(() -> new IllegalArgumentException("Field not found: " + fieldId));

        workflowScreenFieldRepository.delete(field);
        logAudit("DELETE_FIELD", "WORKFLOW_SCREEN_FIELD", fieldId, field.getFieldId(), "Field deleted");
    }

    @Transactional
    public Map<String, Object> updateScreen(UUID screenId, Map<String, Object> updates) {
        WorkflowScreen screen = workflowScreenRepository.findById(screenId)
                .orElseThrow(() -> new IllegalArgumentException("Screen not found: " + screenId));

        if (updates.containsKey("name")) screen.setName((String) updates.get("name"));
        if (updates.containsKey("description")) screen.setDescription((String) updates.get("description"));

        screen = workflowScreenRepository.save(screen);
        logAudit("UPDATE_SCREEN", "WORKFLOW_SCREEN", screenId, screen.getName(), "Screen updated");

        return screenToMap(screen);
    }

    @Transactional
    public void deleteScreen(UUID screenId) {
        WorkflowScreen screen = workflowScreenRepository.findById(screenId)
                .orElseThrow(() -> new IllegalArgumentException("Screen not found: " + screenId));

        workflowScreenTabRepository.deleteByScreenId(screenId);
        workflowScreenRepository.delete(screen);
        logAudit("DELETE_SCREEN", "WORKFLOW_SCREEN", screenId, screen.getName(), "Screen deleted");
    }

    @Transactional
    public List<Map<String, Object>> listScreens(String screenType) {
        if (screenType != null && !screenType.isEmpty()) {
            return workflowScreenRepository.findByScreenType(screenType).stream()
                    .map(this::screenToMap)
                    .collect(Collectors.toList());
        }
        return workflowScreenRepository.findAll().stream()
                .map(this::screenToMap)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getScreen(UUID screenId) {
        return workflowScreenRepository.findById(screenId).map(this::screenToMap);
    }

    // ==================== TRANSITION SCREEN CONFIGURATION ====================

    @Transactional
    public Map<String, Object> assignScreenToTransition(UUID transitionId, Map<String, Object> screenData) {
        WorkflowTransition transition = workflowTransitionRepository.findById(transitionId)
                .orElseThrow(() -> new IllegalArgumentException("Transition not found: " + transitionId));

        UUID screenId = UUID.fromString((String) screenData.get("screenId"));
        transition.setScreenId(screenId);
        workflowTransitionRepository.save(transition);

        logAudit("ASSIGN_SCREEN", "WORKFLOW_TRANSITION", transitionId, transition.getName(), "Screen assigned to transition");
        return Map.of("transitionId", transitionId, "screenId", screenId);
    }

    @Transactional
    public void removeScreenFromTransition(UUID transitionId) {
        WorkflowTransition transition = workflowTransitionRepository.findById(transitionId)
                .orElseThrow(() -> new IllegalArgumentException("Transition not found: " + transitionId));

        transition.setScreenId(null);
        workflowTransitionRepository.save(transition);

        logAudit("REMOVE_SCREEN", "WORKFLOW_TRANSITION", transitionId, transition.getName(), "Screen removed from transition");
    }

    // ==================== MIGRATION & USAGE STATS ====================

    @Transactional
    public Map<String, Object> migrateIssues(UUID workflowId, UUID targetWorkflowId, Map<String, Object> filters) {
        // H8: Delegate to WorkflowMigrationService for actual issue migration
        Map<String, Object> result = new HashMap<>();
        result.put("sourceWorkflowId", workflowId);
        result.put("targetWorkflowId", targetWorkflowId);
        result.put("filters", filters);

        try {
            UUID oldStatusId = filters.get("oldStatusId") != null
                    ? UUID.fromString(String.valueOf(filters.get("oldStatusId"))) : null;
            UUID newStatusId = filters.get("newStatusId") != null
                    ? UUID.fromString(String.valueOf(filters.get("newStatusId"))) : null;
            String userId = filters.get("userId") != null ? String.valueOf(filters.get("userId")) : null;

            if (oldStatusId != null && newStatusId != null) {
                UUID migrationUserId = userId != null ? UUID.fromString(userId) : null;
                WorkflowMigrationResponse migration = workflowMigrationService.createMigration(
                        workflowId, oldStatusId, newStatusId, "WORKFLOW_SWITCH", migrationUserId);
                result.put("migrationId", migration.getId());
                result.put("migrationStatus", migration.getMigrationStatus());
                result.put("issueCount", migration.getIssueCount());
                result.put("status", "migration_created");
                result.put("message", "Migration created. Call startMigration to begin processing.");
            } else {
                result.put("status", "migration_pending");
                result.put("message", "Provide oldStatusId and newStatusId in filters to create a status migration");
            }
        } catch (Exception e) {
            log.warn("Migration creation failed for workflow {}: {}", workflowId, e.getMessage());
            result.put("status", "migration_failed");
            result.put("message", "Migration creation failed: " + e.getMessage());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> previewMigration(UUID workflowId, Map<String, Object> filters) {
        // H8: Use integration client to estimate affected issues via JQL search
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        Map<String, Object> preview = new HashMap<>();
        preview.put("workflowId", workflowId);
        preview.put("workflowName", workflow.getName());
        preview.put("filters", filters);

        // Gather workflow statuses
        List<WorkflowStatus> statuses = workflowStatusRepository.findByWorkflowIdOrderBySequenceAsc(workflowId);
        List<String> statusIds = statuses.stream()
                .map(s -> s.getStatusId().toString())
                .collect(Collectors.toList());
        preview.put("statusCount", statuses.size());
        preview.put("statuses", statusIds);

        // Gather transitions
        List<WorkflowTransition> transitions = workflowTransitionRepository.findByWorkflowId(workflowId);
        preview.put("transitionCount", transitions.size());

        // Estimate affected issues via integration client JQL search
        int estimatedAffectedIssues = 0;
        try {
            if (!statusIds.isEmpty()) {
                String quotedIds = statusIds.stream()
                        .map(id -> "\"" + id + "\"")
                        .collect(Collectors.joining(","));
                String jql = "statusId in (" + quotedIds + ")";
                List<Map<String, Object>> issues = workflowIntegrationClient.searchIssuesJql(jql, 1000);
                estimatedAffectedIssues = issues.size();
            }
        } catch (Exception e) {
            log.warn("Could not estimate affected issues for workflow {}: {}", workflowId, e.getMessage());
        }
        preview.put("estimatedAffectedIssues", estimatedAffectedIssues);

        return preview;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTransitionStats(UUID workflowId, String startDate, String endDate) {
        // H8: Query transition history for real statistics
        Map<String, Object> stats = new HashMap<>();
        stats.put("workflowId", workflowId);
        stats.put("period", Map.of("start", startDate, "end", endDate));

        try {
            List<WorkflowTransitionHistory> historyRecords;
            if (startDate != null && endDate != null) {
                LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
                LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
                historyRecords = workflowTransitionHistoryRepository
                        .findByWorkflowIdAndExecutedAtBetween(workflowId, start, end);
            } else {
                historyRecords = workflowTransitionHistoryRepository.findByWorkflowId(workflowId);
            }

            stats.put("totalTransitions", historyRecords.size());

            // Group by transition name and count occurrences
            Map<String, Long> transitionCounts = historyRecords.stream()
                    .filter(h -> h.getTransitionName() != null)
                    .collect(Collectors.groupingBy(
                            WorkflowTransitionHistory::getTransitionName,
                            Collectors.counting()));

            // Sort by count descending and build top transitions list
            List<Map<String, Object>> topTransitions = transitionCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .map(entry -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("transitionName", entry.getKey());
                        item.put("count", entry.getValue());
                        return item;
                    })
                    .collect(Collectors.toList());

            stats.put("topTransitions", topTransitions);

            // Count successful vs failed transitions
            long successCount = historyRecords.stream()
                    .filter(h -> Boolean.TRUE.equals(h.getSuccess()))
                    .count();
            stats.put("successCount", successCount);
            stats.put("failureCount", historyRecords.size() - successCount);

        } catch (Exception e) {
            log.warn("Failed to compute transition stats for workflow {}: {}", workflowId, e.getMessage());
            stats.put("totalTransitions", 0);
            stats.put("topTransitions", List.of());
            stats.put("error", "Failed to compute stats: " + e.getMessage());
        }

        return stats;
    }

    // ==================== UTILITY METHODS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> validateWorkflow(UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Validate statuses
        if (workflow.getStatuses() == null || workflow.getStatuses().isEmpty()) {
            errors.add("Workflow must have at least one status");
        }

        // Validate transitions
        if (workflow.getTransitions() == null || workflow.getTransitions().isEmpty()) {
            warnings.add("Workflow has no transitions");
        }

        // Validate transition references
        List<WorkflowTransition> transitions = workflowTransitionRepository.findByWorkflowId(workflowId);
        for (WorkflowTransition t : transitions) {
            if (t.getFromStatusId() == null || t.getToStatusId() == null) {
                errors.add("Transition " + t.getName() + " has invalid status references");
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        result.put("warnings", warnings);
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableTransitions(UUID workflowId, UUID statusId) {
        return workflowTransitionRepository.findByWorkflowIdAndFromStatusId(workflowId, statusId).stream()
                .map(this::transitionToMap)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getConditionDefinitions() {
        List<Map<String, Object>> definitions = new ArrayList<>();
        for (String key : pluginRegistry.listConditionKeys()) {
            definitions.add(Map.of("id", key, "name", key, "description", "Registered condition: " + key));
        }
        // Also include conditions defined in the DB
        workflowConditionRepository2.findAll().stream()
                .map(c -> c.getConditionType())
                .distinct()
                .forEach(type -> {
                    if (definitions.stream().noneMatch(d -> type.equals(d.get("id")))) {
                        definitions.add(Map.of("id", type, "name", type, "description", "Database condition: " + type));
                    }
                });
        return definitions;
    }

    public List<Map<String, Object>> getValidatorDefinitions() {
        List<Map<String, Object>> definitions = new ArrayList<>();
        for (String key : pluginRegistry.listValidatorKeys()) {
            definitions.add(Map.of("id", key, "name", key, "description", "Registered validator: " + key));
        }
        workflowValidatorRepository.findAll().stream()
                .map(v -> v.getValidatorType())
                .distinct()
                .forEach(type -> {
                    if (definitions.stream().noneMatch(d -> type.equals(d.get("id")))) {
                        definitions.add(Map.of("id", type, "name", type, "description", "Database validator: " + type));
                    }
                });
        return definitions;
    }

    public List<Map<String, Object>> getPostFunctionDefinitions() {
        List<Map<String, Object>> definitions = new ArrayList<>();
        for (String key : pluginRegistry.listPostFunctionKeys()) {
            definitions.add(Map.of("id", key, "name", key, "description", "Registered post-function: " + key));
        }
        workflowPostFunctionRepository.findAll().stream()
                .map(pf -> pf.getFunctionType())
                .distinct()
                .forEach(type -> {
                    if (definitions.stream().noneMatch(d -> type.equals(d.get("id")))) {
                        definitions.add(Map.of("id", type, "name", type, "description", "Database post-function: " + type));
                    }
                });
        return definitions;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWorkflowAuditLog(UUID workflowId, String action, int page, int size) {
        Page<WorkflowAuditLog> auditLogs = workflowAuditLogRepository.findByEntityTypeOrderByCreatedAtDesc(
                "WORKFLOW", PageRequest.of(page, size));
        return auditLogs.stream()
                .filter(log -> log.getEntityId().equals(workflowId))
                .filter(log -> action == null || action.isEmpty() || log.getAction().equals(action))
                .map(this::auditLogToMap)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllAuditLog(String action, String userId, String startDate, String endDate, int page, int size) {
        Page<WorkflowAuditLog> auditLogs;
        if (action != null && !action.isEmpty()) {
            auditLogs = workflowAuditLogRepository.findByAction(action, PageRequest.of(page, size));
        } else {
            auditLogs = workflowAuditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        }
        return auditLogs.stream().map(this::auditLogToMap).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportWorkflow(UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        Map<String, Object> export = new HashMap<>();
        export.put("version", "1.0");
        export.put("workflow", workflowToMap(workflow));
        export.put("statuses", workflow.getStatuses().stream().map(this::statusToMap).collect(Collectors.toList()));
        export.put("transitions", workflow.getTransitions().stream().map(this::transitionToMap).collect(Collectors.toList()));
        return export;
    }

    @Transactional
    public Map<String, Object> importWorkflow(Map<String, Object> importData) {
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowData = (Map<String, Object>) importData.get("workflow");
        String name = (String) workflowData.get("name");

        if (workflowRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Workflow with name '" + name + "' already exists");
        }

        // H5: Create the workflow, then also import its statuses and transitions
        Map<String, Object> result = createWorkflow(workflowData);
        UUID newWorkflowId = (UUID) result.get("id");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> statuses = (List<Map<String, Object>>) importData.get("statuses");
        if (statuses != null) {
            for (Map<String, Object> s : statuses) {
                try {
                    addStatus(newWorkflowId, s);
                } catch (Exception e) {
                    log.warn("Failed to import status into workflow {}: {}", newWorkflowId, e.getMessage());
                }
            }
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> transitions = (List<Map<String, Object>>) importData.get("transitions");
        if (transitions != null) {
            for (Map<String, Object> t : transitions) {
                try {
                    addTransition(newWorkflowId, t);
                } catch (Exception e) {
                    log.warn("Failed to import transition into workflow {}: {}", newWorkflowId, e.getMessage());
                }
            }
        }

        logAudit("IMPORT", "WORKFLOW", newWorkflowId, name,
                "Imported with " + (statuses != null ? statuses.size() : 0) + " status(es) and "
                + (transitions != null ? transitions.size() : 0) + " transition(s)");

        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> compareVersions(UUID workflowId, int v1, int v2) {
        Optional<WorkflowVersion> version1 = workflowVersionRepository.findByWorkflowIdAndVersionNumber(workflowId, v1);
        Optional<WorkflowVersion> version2 = workflowVersionRepository.findByWorkflowIdAndVersionNumber(workflowId, v2);

        if (version1.isEmpty() || version2.isEmpty()) {
            throw new IllegalArgumentException("One or both versions not found");
        }

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("version1", versionToMap(version1.get()));
        comparison.put("version2", versionToMap(version2.get()));
        comparison.put("snapshot1", version1.get().getStatusesSnapshot());
        comparison.put("snapshot2", version2.get().getStatusesSnapshot());
        return comparison;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSchemes() {
        return workflowSchemeRepository.findAll().stream()
                .map(this::schemeToMap)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getScheme(UUID schemeId) {
        return workflowSchemeRepository.findById(schemeId).map(this::schemeToMap);
    }

    @Transactional
    public void removeWorkflowFromScheme(UUID schemeId, UUID mappingId) {
        WorkflowSchemeMapping mapping = workflowSchemeMappingRepository.findById(mappingId)
                .orElseThrow(() -> new IllegalArgumentException("Mapping not found: " + mappingId));

        workflowSchemeMappingRepository.delete(mapping);
        logAudit("REMOVE_MAPPING", "WORKFLOW_SCHEME_MAPPING", mappingId, "IssueType", "Mapping removed from scheme");
    }

    @Transactional
    public Map<String, Object> setDefaultWorkflow(UUID schemeId, UUID workflowId) {
        WorkflowScheme scheme = workflowSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found: " + schemeId));

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        // Update mappings for this scheme
        List<WorkflowSchemeMapping> mappings = workflowSchemeMappingRepository.findBySchemeId(schemeId);
        for (WorkflowSchemeMapping m : mappings) {
            if (m.getIssueTypeId() == null) {
                m.setWorkflow(workflow);
                workflowSchemeMappingRepository.save(m);
            }
        }

        logAudit("SET_DEFAULT", "WORKFLOW_SCHEME", schemeId, scheme.getName(), "Default workflow set to " + workflow.getName());
        return schemeToMap(scheme);
    }

    // ==================== UTILITY METHODS ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getWorkflowUsage(UUID workflowId) {
        List<WorkflowSchemeMapping> mappings = workflowSchemeMappingRepository.findByWorkflowId(workflowId);
        List<String> schemes = mappings.stream()
                .map(m -> m.getScheme().getName())
                .collect(Collectors.toList());

        Map<String, Object> usage = new HashMap<>();
        usage.put("workflowId", workflowId);
        usage.put("schemeCount", mappings.size());
        usage.put("schemes", schemes);
        usage.put("isSystem", workflowRepository.findById(workflowId).map(Workflow::getIsSystem).orElse(false));

        return usage;
    }

    private void createVersionSnapshot(Workflow workflow, String description, String changeType) {
        int maxVersion = workflowVersionRepository.findMaxVersionNumber(workflow.getId()).orElse(0);
        int newVersion = maxVersion + 1;

        try {
            // Collect all normalized conditions, validators, and post-functions for transitions in this workflow
            List<WorkflowTransition> transitions = workflowTransitionRepository.findByWorkflowId(workflow.getId());
            List<UUID> transitionIds = transitions.stream().map(WorkflowTransition::getId).collect(Collectors.toList());

            List<Map<String, Object>> conditionsList = new ArrayList<>();
            List<Map<String, Object>> validatorsList = new ArrayList<>();
            List<Map<String, Object>> postFunctionsList = new ArrayList<>();
            for (UUID tId : transitionIds) {
                workflowConditionRepository2.findByTransitionIdOrderBySequenceAsc(tId).forEach(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId());
                    m.put("transitionId", c.getTransitionId());
                    m.put("conditionType", c.getConditionType());
                    m.put("fieldName", c.getFieldName());
                    m.put("operator", c.getOperator());
                    m.put("value", c.getValue());
                    m.put("negate", c.getNegate());
                    m.put("sequence", c.getSequence());
                    conditionsList.add(m);
                });
                workflowValidatorRepository.findByTransitionIdOrderBySequenceAsc(tId).forEach(v -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", v.getId());
                    m.put("transitionId", v.getTransitionId());
                    m.put("validatorType", v.getValidatorType());
                    m.put("fieldName", v.getFieldName());
                    m.put("errorMessage", v.getErrorMessage());
                    m.put("sequence", v.getSequence());
                    validatorsList.add(m);
                });
                workflowPostFunctionRepository.findByTransitionIdOrderBySequenceAsc(tId).forEach(pf -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", pf.getId());
                    m.put("transitionId", pf.getTransitionId());
                    m.put("functionType", pf.getFunctionType());
                    m.put("sequence", pf.getSequence());
                    m.put("enabled", pf.getEnabled());
                    postFunctionsList.add(m);
                });
            }

            WorkflowVersion version = WorkflowVersion.builder()
                    .workflow(workflow)
                    .versionNumber(newVersion)
                    .workflowSnapshot(workflow.getStatusCategoryMapping())
                    .statusesSnapshot(objectMapper.writeValueAsString(
                            workflow.getStatuses().stream().map(this::statusToMap).collect(Collectors.toList())))
                    .transitionsSnapshot(objectMapper.writeValueAsString(
                            workflow.getTransitions().stream().map(this::transitionToMap).collect(Collectors.toList())))
                    .conditionsSnapshot(objectMapper.writeValueAsString(conditionsList))
                    .validatorsSnapshot(objectMapper.writeValueAsString(validatorsList))
                    .postFunctionsSnapshot(objectMapper.writeValueAsString(postFunctionsList))
                    .changeDescription(description)
                    .changeType(changeType)
                    .build();

            workflowVersionRepository.save(version);
        } catch (Exception e) {
            log.error("Failed to create version snapshot", e);
            throw new RuntimeException("Failed to create version snapshot", e);
        }
    }

    private void cloneWorkflowStatuses(UUID sourceWorkflowId, UUID targetWorkflowId) {
        List<WorkflowStatus> sourceStatuses = workflowStatusRepository.findByWorkflowIdOrderBySequenceAsc(sourceWorkflowId);
        for (WorkflowStatus source : sourceStatuses) {
            WorkflowStatus clone = WorkflowStatus.builder()
                    .workflowId(targetWorkflowId)
                    .statusId(source.getStatusId())
                    .sequence(source.getSequence())
                    .build();
            workflowStatusRepository.save(clone);
        }
    }

    private void cloneWorkflowTransitions(UUID sourceWorkflowId, UUID targetWorkflowId) {
        List<WorkflowTransition> sourceTransitions = workflowTransitionRepository.findByWorkflowId(sourceWorkflowId);
        for (WorkflowTransition source : sourceTransitions) {
            WorkflowTransition clone = WorkflowTransition.builder()
                    .workflowId(targetWorkflowId)
                    .fromStatusId(source.getFromStatusId())
                    .toStatusId(source.getToStatusId())
                    .name(source.getName())
                    .description(source.getDescription())
                    .displayOrder(source.getDisplayOrder())
                    .type(source.getType())
                    .conditionConditions(source.getConditionConditions())
                    .validatorValidators(source.getValidatorValidators())
                    .postFunctionFunctions(source.getPostFunctionFunctions())
                    .build();
            WorkflowTransition savedClone = workflowTransitionRepository.save(clone);

            // H7: Also clone normalized condition/validator/post-function entities
            copyConditions(source.getId(), savedClone.getId());
            copyValidators(source.getId(), savedClone.getId());
            copyPostFunctions(source.getId(), savedClone.getId());
        }
    }

    private void copyConditions(UUID fromTransitionId, UUID toTransitionId) {
        for (WorkflowCondition src : workflowConditionRepository2.findByTransitionIdOrderBySequenceAsc(fromTransitionId)) {
            WorkflowCondition copy = WorkflowCondition.builder()
                    .transitionId(toTransitionId)
                    .conditionType(src.getConditionType())
                    .fieldName(src.getFieldName())
                    .operator(src.getOperator())
                    .value(src.getValue())
                    .conditionData(src.getConditionData())
                    .negate(src.getNegate())
                    .sequence(src.getSequence())
                    .build();
            workflowConditionRepository2.save(copy);
        }
    }

    private void copyValidators(UUID fromTransitionId, UUID toTransitionId) {
        for (WorkflowValidator src : workflowValidatorRepository.findByTransitionIdOrderBySequenceAsc(fromTransitionId)) {
            WorkflowValidator copy = WorkflowValidator.builder()
                    .transitionId(toTransitionId)
                    .validatorType(src.getValidatorType())
                    .fieldName(src.getFieldName())
                    .validatorData(src.getValidatorData())
                    .errorMessage(src.getErrorMessage())
                    .sequence(src.getSequence())
                    .build();
            workflowValidatorRepository.save(copy);
        }
    }

    private void copyPostFunctions(UUID fromTransitionId, UUID toTransitionId) {
        for (WorkflowPostFunction src : workflowPostFunctionRepository.findByTransitionIdOrderBySequenceAsc(fromTransitionId)) {
            WorkflowPostFunction copy = WorkflowPostFunction.builder()
                    .transitionId(toTransitionId)
                    .functionType(src.getFunctionType())
                    .functionData(src.getFunctionData())
                    .sequence(src.getSequence())
                    .enabled(src.getEnabled())
                    .build();
            workflowPostFunctionRepository.save(copy);
        }
    }

    private void logAudit(String action, String category, UUID entityId, String entityName, String details) {
        try {
            WorkflowAuditLog auditLog = WorkflowAuditLog.builder()
                    .action(action)
                    .entityType(category)
                    .entityId(entityId)
                    .entityName(entityName)
                    .details(objectMapper.writeValueAsString(Map.of("details", details)))
                    .build();
            workflowAuditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log audit", e);
        }
    }

    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize to JSON", e);
            return "{}";
        }
    }

    private Map<String, String> getStatusCategoryMapping() {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("TODO", statusCategoryColorTodo);
        mapping.put("IN_PROGRESS", statusCategoryColorInProgress);
        mapping.put("DONE", statusCategoryColorDone);
        return mapping;
    }

    private Map<String, Object> workflowToMap(Workflow workflow) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", workflow.getId());
        map.put("name", workflow.getName());
        map.put("description", workflow.getDescription());
        map.put("isSystem", workflow.getIsSystem());
        map.put("isActive", workflow.getIsActive());
        map.put("isDraft", workflow.getIsDraft());
        map.put("isDefault", workflow.getIsDefault());
        map.put("projectId", workflow.getProjectId());
        map.put("draftOfWorkflowId", workflow.getDraftOfWorkflowId());
        map.put("type", workflow.getType());
        map.put("publishedAt", workflow.getPublishedAt());
        map.put("createdAt", workflow.getCreatedAt());
        map.put("updatedAt", workflow.getUpdatedAt());
        // Count actual statuses and transitions from DB
        map.put("statusCount", workflowStatusRepository.findByWorkflowIdOrderBySequenceAsc(workflow.getId()).size());
        map.put("transitionCount", workflowTransitionRepository.findByWorkflowId(workflow.getId()).size());
        return map;
    }

    private Map<String, Object> statusToMap(WorkflowStatus status) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", status.getId());
        map.put("workflowId", status.getWorkflowId());
        map.put("statusId", status.getStatusId());
        map.put("sequence", status.getSequence());
        map.put("createdAt", status.getCreatedAt());
        return map;
    }

    private Map<String, Object> transitionToMap(WorkflowTransition transition) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", transition.getId());
        map.put("workflowId", transition.getWorkflowId());
        map.put("name", transition.getName());
        map.put("description", transition.getDescription());
        map.put("fromStatusId", transition.getFromStatusId());
        map.put("toStatusId", transition.getToStatusId());
        map.put("displayOrder", transition.getDisplayOrder());
        map.put("type", transition.getType());
        map.put("conditions", transition.getConditionConditions());
        map.put("validators", transition.getValidatorValidators());
        map.put("postFunctions", transition.getPostFunctionFunctions());
        map.put("screenId", transition.getScreenId());
        return map;
    }

    private Map<String, Object> schemeToMap(WorkflowScheme scheme) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", scheme.getId());
        map.put("name", scheme.getName());
        map.put("description", scheme.getDescription());
        map.put("isDefault", scheme.getIsDefault());
        map.put("createdAt", scheme.getCreatedAt());
        map.put("updatedAt", scheme.getUpdatedAt());
        map.put("mappingCount", workflowSchemeMappingRepository.countBySchemeId(scheme.getId()));
        return map;
    }

    private Map<String, Object> versionToMap(WorkflowVersion version) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", version.getId());
        map.put("versionNumber", version.getVersionNumber());
        map.put("changeDescription", version.getChangeDescription());
        map.put("changeType", version.getChangeType());
        map.put("createdAt", version.getCreatedAt());
        map.put("createdBy", version.getCreatedBy());
        return map;
    }

    private Map<String, Object> screenToMap(WorkflowScreen screen) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", screen.getId());
        map.put("name", screen.getName());
        map.put("description", screen.getDescription());
        map.put("screenType", screen.getScreenType());
        map.put("isSystem", screen.getIsSystem());
        map.put("isDefault", screen.getIsDefault());
        map.put("tabCount", screen.getTabs() != null ? screen.getTabs().size() : 0);
        return map;
    }

    private Map<String, Object> tabToMap(WorkflowScreenTab tab) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", tab.getId());
        map.put("tabName", tab.getTabName());
        map.put("description", tab.getDescription());
        map.put("orderIndex", tab.getOrderIndex());
        map.put("fieldCount", tab.getFields() != null ? tab.getFields().size() : 0);
        return map;
    }

    private Map<String, Object> fieldToMap(WorkflowScreenField field) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", field.getId());
        map.put("fieldId", field.getFieldId());
        map.put("fieldLabel", field.getFieldLabel());
        map.put("fieldType", field.getFieldType());
        map.put("required", field.getRequired());
        map.put("hidden", field.getHidden());
        map.put("readonly", field.getReadonly());
        map.put("orderIndex", field.getOrderIndex());
        return map;
    }

    private Map<String, Object> auditLogToMap(WorkflowAuditLog log) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", log.getId());
        map.put("action", log.getAction());
        map.put("entityType", log.getEntityType());
        map.put("entityId", log.getEntityId());
        map.put("entityName", log.getEntityName());
        map.put("details", log.getDetails());
        map.put("userId", log.getUserId());
        map.put("createdAt", log.getCreatedAt());
        return map;
    }
}