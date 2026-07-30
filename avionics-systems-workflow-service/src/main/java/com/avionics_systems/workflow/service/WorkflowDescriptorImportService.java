package com.avionics_systems.workflow.service;

import com.avionics_systems.workflow.dto.ImportWorkflowDescriptorRequest;
import com.avionics_systems.workflow.dto.WorkflowResponse;
import com.avionics_systems.workflow.entity.*;
import com.avionics_systems.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowDescriptorImportService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStatusRepository workflowStatusRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowConditionRepository workflowConditionRepository;
    private final WorkflowValidatorRepository workflowValidatorRepository;
    private final WorkflowPostFunctionRepository workflowPostFunctionRepository;
    private final WorkflowLayoutEdgeSyncService workflowLayoutEdgeSyncService;

    @Transactional
    public WorkflowResponse importDescriptor(ImportWorkflowDescriptorRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Workflow name is required");
        }
        if (workflowRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Workflow already exists: " + request.getName());
        }

        Workflow workflow = Workflow.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .description(request.getDescription())
                .isDefault(request.isMakeDefault())
                .build();
        workflow = workflowRepository.save(workflow);
        UUID workflowId = workflow.getId();

        int seq = 0;
        for (ImportWorkflowDescriptorRequest.StepImport step : request.getSteps()) {
            if (step.getPlatformStatusId() == null) {
                continue;
            }
            WorkflowStatus ws = WorkflowStatus.builder()
                    .workflowId(workflowId)
                    .statusId(step.getPlatformStatusId())
                    .sequence(step.getSequence() >= 0 ? step.getSequence() : seq++)
                    .build();
            workflowStatusRepository.save(ws);
        }

        for (ImportWorkflowDescriptorRequest.TransitionImport t : request.getTransitions()) {
            if (t.getFromStatusId() == null || t.getToStatusId() == null) {
                continue;
            }
            WorkflowTransition transition = WorkflowTransition.builder()
                    .workflowId(workflowId)
                    .name(t.getName())
                    .fromStatusId(t.getFromStatusId())
                    .toStatusId(t.getToStatusId())
                    .screenId(parseScreenId(t.getScreenId()))
                    .type(t.isGlobal() ? "GLOBAL" : "MANUAL")
                    .build();
            transition = workflowTransitionRepository.save(transition);
            UUID transitionId = transition.getId();

            addComponents(t.getValidators(), transitionId, true);
            addComponents(t.getConditions(), transitionId, false);
            addPostFunctions(t.getPostFunctions(), transitionId);
        }

        workflowLayoutEdgeSyncService.syncLayoutEdges(workflowId);
        log.info("Imported workflow descriptor {} with {} transitions", workflowId, request.getTransitions().size());

        return WorkflowResponse.builder()
                .id(workflowId)
                .projectId(workflow.getProjectId())
                .name(workflow.getName())
                .description(workflow.getDescription())
                .isDefault(workflow.getIsDefault())
                .build();
    }

    private void addComponents(List<ImportWorkflowDescriptorRequest.ComponentImport> items, UUID transitionId, boolean validator) {
        if (items == null) {
            return;
        }
        for (ImportWorkflowDescriptorRequest.ComponentImport c : items) {
            if (c.getType() == null || "UNSUPPORTED".equals(c.getType())) {
                continue;
            }
            if (validator) {
                workflowValidatorRepository.save(WorkflowValidator.builder()
                        .transitionId(transitionId)
                        .validatorType(c.getType())
                        .fieldName(c.getFieldName())
                        .validatorData(c.getConfigJson())
                        .sequence(c.getSequence())
                        .build());
            } else {
                workflowConditionRepository.save(WorkflowCondition.builder()
                        .transitionId(transitionId)
                        .conditionType(c.getType())
                        .fieldName(c.getFieldName())
                        .value(c.getValue())
                        .conditionData(c.getConfigJson())
                        .sequence(c.getSequence())
                        .build());
            }
        }
    }

    private static UUID parseScreenId(String screenId) {
        if (screenId == null || screenId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(screenId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void addPostFunctions(List<ImportWorkflowDescriptorRequest.ComponentImport> items, UUID transitionId) {
        if (items == null) {
            return;
        }
        for (ImportWorkflowDescriptorRequest.ComponentImport c : items) {
            if (c.getType() == null || "UNSUPPORTED".equals(c.getType())) {
                continue;
            }
            workflowPostFunctionRepository.save(WorkflowPostFunction.builder()
                    .transitionId(transitionId)
                    .functionType(c.getType())
                    .functionData(c.getConfigJson())
                    .sequence(c.getSequence())
                    .build());
        }
    }
}
