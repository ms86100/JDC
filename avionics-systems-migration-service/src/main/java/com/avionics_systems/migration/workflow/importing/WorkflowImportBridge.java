package com.avionics_systems.migration.workflow.importing;

import com.avionics_systems.migration.service.clients.WorkflowServiceClient;
import com.avionics_systems.migration.service.clients.dto.ImportWorkflowDescriptorRequest;
import com.avionics_systems.migration.service.clients.dto.WorkflowResponse;
import com.avionics_systems.migration.workflow.graph.WorkflowGraph;
import com.avionics_systems.migration.workflow.model.*;
import com.avionics_systems.migration.workflow.registry.OsWorkflowDescriptorRegistry;
import com.avionics_systems.migration.workflow.support.WorkflowXmlStatusMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowImportBridge {

    @Value("${app.workflow.default-import-description:Imported from Legacy DC XML}")
    private String defaultWorkflowDescription;

    private final WorkflowServiceClient workflowServiceClient;
    private final WorkflowXmlStatusMapper statusMapper;
    private final OsWorkflowDescriptorRegistry registry;

    public WorkflowResponse pushToWorkflowService(WorkflowDescriptorModel descriptor, WorkflowGraph graph,
                                                    UUID projectId, boolean makeDefault) {
        Map<String, UUID> stepPlatformStatus = new HashMap<>();
        List<ImportWorkflowDescriptorRequest.StepImport> steps = new ArrayList<>();
        int seq = 0;
        for (WorkflowStepModel step : descriptor.getSteps()) {
            UUID platformId = statusMapper.resolvePlatformStatusId(
                    step.getMeta().get("legacy.status.id"),
                    statusMapper.resolveStatusName(step.getMeta().get("legacy.status.id"), step.getName(), step.getMeta()));
            stepPlatformStatus.put(step.getId(), platformId);
            steps.add(new ImportWorkflowDescriptorRequest.StepImport(
                    step.getId(), step.getName(), platformId, seq++));
        }

        List<ImportWorkflowDescriptorRequest.TransitionImport> transitions = new ArrayList<>();
        collectTransitions(descriptor.getSteps(), stepPlatformStatus, transitions, false);
        collectTransitionList(descriptor.getCommonActions(), stepPlatformStatus, transitions, true);
        collectTransitionList(descriptor.getInitialActions(), stepPlatformStatus, transitions, false);

        ImportWorkflowDescriptorRequest request = ImportWorkflowDescriptorRequest.builder()
                .projectId(projectId)
                .name(descriptor.getName())
                .description(descriptor.getMeta().getOrDefault("legacy.description", defaultWorkflowDescription))
                .makeDefault(makeDefault)
                .steps(steps)
                .transitions(transitions)
                .build();

        return workflowServiceClient.importWorkflowDescriptor(request);
    }

    private void collectTransitions(List<WorkflowStepModel> stepModels, Map<String, UUID> stepStatus,
                                    List<ImportWorkflowDescriptorRequest.TransitionImport> out, boolean global) {
        for (WorkflowStepModel step : stepModels) {
            for (WorkflowActionModel action : step.getActions()) {
                addTransition(action, step.getId(), stepStatus, out, global);
            }
        }
    }

    private void collectTransitionList(List<WorkflowActionModel> actions, Map<String, UUID> stepStatus,
                                       List<ImportWorkflowDescriptorRequest.TransitionImport> out, boolean global) {
        for (WorkflowActionModel action : actions) {
            String from = action.getSourceStepId();
            if (global) {
                for (String stepId : stepStatus.keySet()) {
                    addTransitionWithFrom(action, stepId, stepStatus, out, true);
                }
            } else {
                addTransition(action, from, stepStatus, out, false);
            }
        }
    }

    private void addTransition(WorkflowActionModel action, String fromStepId, Map<String, UUID> stepStatus,
                               List<ImportWorkflowDescriptorRequest.TransitionImport> out, boolean global) {
        addTransitionWithFrom(action, fromStepId, stepStatus, out, global);
    }

    private void addTransitionWithFrom(WorkflowActionModel action, String fromStepId,
                                       Map<String, UUID> stepStatus,
                                       List<ImportWorkflowDescriptorRequest.TransitionImport> out, boolean global) {
        UUID fromStatus = fromStepId != null ? stepStatus.get(fromStepId) : null;
        for (WorkflowResultModel result : action.getResults()) {
            UUID toStatus = stepStatus.get(result.getTargetStepId());
            if (toStatus == null && fromStatus == null && global) {
                continue;
            }
            if (toStatus == null) {
                continue;
            }
            if (fromStatus == null && !action.isInitial()) {
                continue;
            }
            out.add(new ImportWorkflowDescriptorRequest.TransitionImport(
                    action.getId(),
                    action.getName(),
                    fromStatus,
                    toStatus,
                    action.getView(),
                    global,
                    mapValidators(action),
                    mapConditions(action),
                    mapPostFunctions(action)
            ));
        }
    }

    private List<ImportWorkflowDescriptorRequest.ComponentImport> mapValidators(WorkflowActionModel action) {
        List<ImportWorkflowDescriptorRequest.ComponentImport> list = new ArrayList<>();
        int seq = 0;
        for (WorkflowFunctionDescriptor v : action.getValidators()) {
            var m = registry.mapValidator(v);
            list.add(new ImportWorkflowDescriptorRequest.ComponentImport(
                    m.type(), m.fieldName(), null, m.configJson(), seq++));
        }
        return list;
    }

    private List<ImportWorkflowDescriptorRequest.ComponentImport> mapConditions(WorkflowActionModel action) {
        List<ImportWorkflowDescriptorRequest.ComponentImport> list = new ArrayList<>();
        int seq = 0;
        for (WorkflowFunctionDescriptor c : action.getConditions()) {
            var m = registry.mapCondition(c);
            list.add(new ImportWorkflowDescriptorRequest.ComponentImport(
                    m.type(), m.fieldName(), m.value(), m.configJson(), seq++));
        }
        return list;
    }

    private List<ImportWorkflowDescriptorRequest.ComponentImport> mapPostFunctions(WorkflowActionModel action) {
        List<ImportWorkflowDescriptorRequest.ComponentImport> list = new ArrayList<>();
        int seq = 0;
        for (WorkflowFunctionDescriptor pf : action.getPostFunctions()) {
            var m = registry.mapPostFunction(pf);
            list.add(new ImportWorkflowDescriptorRequest.ComponentImport(
                    m.type(), null, null, m.configJson(), seq++));
        }
        return list;
    }
}
