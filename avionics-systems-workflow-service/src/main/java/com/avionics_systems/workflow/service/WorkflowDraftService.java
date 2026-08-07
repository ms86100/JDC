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

        UUID wfId = workflow.getId();

        // Serialize statuses
        List<WorkflowStatus> statuses = workflowStatusRepository.findByWorkflowIdOrderBySequenceAsc(wfId);
        data.put("statuses", statuses.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("statusId", s.getStatusId().toString());
            m.put("sequence", s.getSequence());
            return m;
        }).collect(Collectors.toList()));

        // Serialize transitions with conditions, validators, post-functions, and properties
        List<WorkflowTransition> transitions = workflowTransitionRepository.findByWorkflowId(wfId);
        List<Map<String, Object>> transitionList = transitions.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", t.getName());
            m.put("description", t.getDescription());
            m.put("fromStatusId", t.getFromStatusId() != null ? t.getFromStatusId().toString() : null);
            m.put("toStatusId", t.getToStatusId() != null ? t.getToStatusId().toString() : null);
            m.put("displayOrder", t.getDisplayOrder());
            m.put("icon", t.getIcon());
            m.put("type", t.getType());
            m.put("triggerType", t.getTriggerType());
            m.put("triggerConfig", t.getTriggerConfig());
            m.put("origin", t.getOrigin());
            m.put("requiresApproval", t.getRequiresApproval());
            m.put("approvalGroupId", t.getApprovalGroupId() != null ? t.getApprovalGroupId().toString() : null);
            m.put("allowAssigneeOverride", t.getAllowAssigneeOverride());
            m.put("allowUnassign", t.getAllowUnassign());
            m.put("fieldsRequired", t.getFieldsRequired());
            m.put("fieldsUpdated", t.getFieldsUpdated());
            m.put("fieldsHidden", t.getFieldsHidden());
            m.put("fieldsAutoSubmit", t.getFieldsAutoSubmit());
            m.put("permissionCheck", t.getPermissionCheck());
            m.put("userGroupIds", t.getUserGroupIds());
            m.put("remoteLinkTransition", t.getRemoteLinkTransition());
            m.put("remoteLinkDirection", t.getRemoteLinkDirection());
            m.put("remoteLinkIssueLinkType", t.getRemoteLinkIssueLinkType());
            m.put("allowLoop", t.getAllowLoop());
            m.put("maxLoopCount", t.getMaxLoopCount());
            m.put("conditionConditions", t.getConditionConditions());
            m.put("conditionOperator", t.getConditionOperator());
            m.put("validatorValidators", t.getValidatorValidators());
            m.put("postFunctionFunctions", t.getPostFunctionFunctions());
            m.put("screenId", t.getScreenId() != null ? t.getScreenId().toString() : null);

            // Serialize normalized conditions
            List<WorkflowCondition> conditions = workflowConditionRepository.findByTransitionIdOrderBySequenceAsc(t.getId());
            m.put("conditions", conditions.stream().map(c -> {
                Map<String, Object> cm = new HashMap<>();
                cm.put("conditionType", c.getConditionType());
                cm.put("fieldName", c.getFieldName());
                cm.put("operator", c.getOperator());
                cm.put("value", c.getValue());
                cm.put("conditionData", c.getConditionData());
                cm.put("negate", c.getNegate());
                cm.put("sequence", c.getSequence());
                return cm;
            }).collect(Collectors.toList()));

            // Serialize normalized validators
            List<WorkflowValidator> validators = workflowValidatorRepository.findByTransitionIdOrderBySequenceAsc(t.getId());
            m.put("validators", validators.stream().map(v -> {
                Map<String, Object> vm = new HashMap<>();
                vm.put("validatorType", v.getValidatorType());
                vm.put("fieldName", v.getFieldName());
                vm.put("validatorData", v.getValidatorData());
                vm.put("errorMessage", v.getErrorMessage());
                vm.put("sequence", v.getSequence());
                vm.put("continueOnError", v.getContinueOnError());
                return vm;
            }).collect(Collectors.toList()));

            // Serialize normalized post-functions
            List<WorkflowPostFunction> postFunctions = workflowPostFunctionRepository.findByTransitionIdOrderBySequenceAsc(t.getId());
            m.put("postFunctions", postFunctions.stream().map(pf -> {
                Map<String, Object> pm = new HashMap<>();
                pm.put("functionType", pf.getFunctionType());
                pm.put("functionData", pf.getFunctionData());
                pm.put("sequence", pf.getSequence());
                pm.put("enabled", pf.getEnabled());
                pm.put("continueOnError", pf.getContinueOnError());
                pm.put("async", pf.getAsync());
                pm.put("failOnError", pf.getFailOnError());
                return pm;
            }).collect(Collectors.toList()));

            // Serialize transition properties
            List<WorkflowTransitionProperty> properties = workflowTransitionPropertyRepository.findByTransitionId(t.getId());
            m.put("properties", properties.stream().map(p -> {
                Map<String, Object> pp = new HashMap<>();
                pp.put("propertyKey", p.getPropertyKey());
                pp.put("propertyValue", p.getPropertyValue());
                pp.put("propertyType", p.getPropertyType());
                pp.put("isSystem", p.getIsSystem());
                return pp;
            }).collect(Collectors.toList()));

            return m;
        }).collect(Collectors.toList());

        data.put("transitions", transitionList);

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

        try {
            String snapshotJson = objectMapper.writeValueAsString(snapshot);

            WorkflowVersion version = WorkflowVersion.builder()
                    .workflow(workflow)
                    .versionNumber(versionNumber)
                    .workflowSnapshot(snapshotJson)
                    .statusesSnapshot(objectMapper.writeValueAsString(snapshot.get("statuses")))
                    .transitionsSnapshot(objectMapper.writeValueAsString(snapshot.get("transitions")))
                    .createdBy(userId)
                    .changeDescription(changeDescription)
                    .changeType("UPDATE")
                    .build();

            workflowVersionRepository.save(version);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize workflow snapshot", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void applyDraftChanges(Workflow workflow, String draftData) {
        try {
            Map<String, Object> data = objectMapper.readValue(draftData, Map.class);

            // Restore metadata
            if (data.containsKey("name")) {
                workflow.setName((String) data.get("name"));
            }
            if (data.containsKey("description")) {
                workflow.setDescription((String) data.get("description"));
            }
            if (data.containsKey("statusCategoryMapping")) {
                workflow.setStatusCategoryMapping((String) data.get("statusCategoryMapping"));
            }
            if (data.containsKey("type")) {
                workflow.setType((String) data.get("type"));
            }
            workflowRepository.save(workflow);

            UUID wfId = workflow.getId();

            // Restore statuses
            if (data.containsKey("statuses")) {
                workflowStatusRepository.deleteByWorkflowId(wfId);
                List<Map<String, Object>> statuses = (List<Map<String, Object>>) data.get("statuses");
                for (Map<String, Object> s : statuses) {
                    WorkflowStatus status = WorkflowStatus.builder()
                            .workflowId(wfId)
                            .statusId(UUID.fromString((String) s.get("statusId")))
                            .sequence((Integer) s.get("sequence"))
                            .build();
                    workflowStatusRepository.save(status);
                }
            }

            // Restore transitions and their sub-entities
            if (data.containsKey("transitions")) {
                // Delete existing transitions and their sub-entities
                List<WorkflowTransition> existingTransitions = workflowTransitionRepository.findByWorkflowId(wfId);
                for (WorkflowTransition existing : existingTransitions) {
                    workflowConditionRepository.deleteByTransitionId(existing.getId());
                    workflowValidatorRepository.deleteByTransitionId(existing.getId());
                    workflowPostFunctionRepository.deleteByTransitionId(existing.getId());
                    workflowTransitionPropertyRepository.deleteByTransitionId(existing.getId());
                }
                workflowTransitionRepository.deleteByWorkflowId(wfId);

                // Recreate transitions from draft data
                List<Map<String, Object>> transitions = (List<Map<String, Object>>) data.get("transitions");
                for (Map<String, Object> t : transitions) {
                    WorkflowTransition transition = WorkflowTransition.builder()
                            .workflowId(wfId)
                            .name((String) t.get("name"))
                            .description((String) t.get("description"))
                            .fromStatusId(t.get("fromStatusId") != null ? UUID.fromString((String) t.get("fromStatusId")) : null)
                            .toStatusId(t.get("toStatusId") != null ? UUID.fromString((String) t.get("toStatusId")) : null)
                            .displayOrder((Integer) t.get("displayOrder"))
                            .icon((String) t.get("icon"))
                            .type((String) t.get("type"))
                            .triggerType((String) t.get("triggerType"))
                            .triggerConfig((Map<String, Object>) t.get("triggerConfig"))
                            .origin((String) t.get("origin"))
                            .requiresApproval((Boolean) t.get("requiresApproval"))
                            .approvalGroupId(t.get("approvalGroupId") != null ? UUID.fromString((String) t.get("approvalGroupId")) : null)
                            .allowAssigneeOverride((Boolean) t.get("allowAssigneeOverride"))
                            .allowUnassign((Boolean) t.get("allowUnassign"))
                            .fieldsRequired((List<String>) t.get("fieldsRequired"))
                            .fieldsUpdated((List<Map<String, Object>>) t.get("fieldsUpdated"))
                            .fieldsHidden((List<String>) t.get("fieldsHidden"))
                            .fieldsAutoSubmit((Boolean) t.get("fieldsAutoSubmit"))
                            .permissionCheck((String) t.get("permissionCheck"))
                            .userGroupIds((List<String>) t.get("userGroupIds"))
                            .remoteLinkTransition((Boolean) t.get("remoteLinkTransition"))
                            .remoteLinkDirection((String) t.get("remoteLinkDirection"))
                            .remoteLinkIssueLinkType((String) t.get("remoteLinkIssueLinkType"))
                            .allowLoop((Boolean) t.get("allowLoop"))
                            .maxLoopCount((Integer) t.get("maxLoopCount"))
                            .conditionConditions((List<Map<String, Object>>) t.get("conditionConditions"))
                            .conditionOperator((String) t.get("conditionOperator"))
                            .validatorValidators((List<Map<String, Object>>) t.get("validatorValidators"))
                            .postFunctionFunctions((List<Map<String, Object>>) t.get("postFunctionFunctions"))
                            .screenId(t.get("screenId") != null ? UUID.fromString((String) t.get("screenId")) : null)
                            .build();
                    transition = workflowTransitionRepository.save(transition);

                    // Restore normalized conditions
                    if (t.containsKey("conditions")) {
                        List<Map<String, Object>> conditions = (List<Map<String, Object>>) t.get("conditions");
                        for (Map<String, Object> c : conditions) {
                            WorkflowCondition condition = WorkflowCondition.builder()
                                    .transitionId(transition.getId())
                                    .conditionType((String) c.get("conditionType"))
                                    .fieldName((String) c.get("fieldName"))
                                    .operator((String) c.get("operator"))
                                    .value((String) c.get("value"))
                                    .conditionData((String) c.get("conditionData"))
                                    .negate((Boolean) c.get("negate"))
                                    .sequence((Integer) c.get("sequence"))
                                    .build();
                            workflowConditionRepository.save(condition);
                        }
                    }

                    // Restore normalized validators
                    if (t.containsKey("validators")) {
                        List<Map<String, Object>> validators = (List<Map<String, Object>>) t.get("validators");
                        for (Map<String, Object> v : validators) {
                            WorkflowValidator validator = WorkflowValidator.builder()
                                    .transitionId(transition.getId())
                                    .validatorType((String) v.get("validatorType"))
                                    .fieldName((String) v.get("fieldName"))
                                    .validatorData((String) v.get("validatorData"))
                                    .errorMessage((String) v.get("errorMessage"))
                                    .sequence((Integer) v.get("sequence"))
                                    .continueOnError((Boolean) v.get("continueOnError"))
                                    .build();
                            workflowValidatorRepository.save(validator);
                        }
                    }

                    // Restore normalized post-functions
                    if (t.containsKey("postFunctions")) {
                        List<Map<String, Object>> postFunctions = (List<Map<String, Object>>) t.get("postFunctions");
                        for (Map<String, Object> pf : postFunctions) {
                            WorkflowPostFunction postFunction = WorkflowPostFunction.builder()
                                    .transitionId(transition.getId())
                                    .functionType((String) pf.get("functionType"))
                                    .functionData((String) pf.get("functionData"))
                                    .sequence((Integer) pf.get("sequence"))
                                    .enabled((Boolean) pf.get("enabled"))
                                    .continueOnError((Boolean) pf.get("continueOnError"))
                                    .async((Boolean) pf.get("async"))
                                    .failOnError((Boolean) pf.get("failOnError"))
                                    .build();
                            workflowPostFunctionRepository.save(postFunction);
                        }
                    }

                    // Restore transition properties
                    if (t.containsKey("properties")) {
                        List<Map<String, Object>> properties = (List<Map<String, Object>>) t.get("properties");
                        for (Map<String, Object> p : properties) {
                            WorkflowTransitionProperty property = WorkflowTransitionProperty.builder()
                                    .transitionId(transition.getId())
                                    .propertyKey((String) p.get("propertyKey"))
                                    .propertyValue((String) p.get("propertyValue"))
                                    .propertyType((String) p.get("propertyType"))
                                    .isSystem((Boolean) p.get("isSystem"))
                                    .build();
                            workflowTransitionPropertyRepository.save(property);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply draft changes: " + e.getMessage(), e);
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