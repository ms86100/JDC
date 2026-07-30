package com.avionics_systems.migration.workflow.validation;

import com.avionics_systems.migration.workflow.graph.WorkflowGraph;
import com.avionics_systems.migration.workflow.graph.WorkflowGraphBuilder;
import com.avionics_systems.migration.workflow.model.*;
import com.avionics_systems.migration.workflow.registry.OsWorkflowDescriptorRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class WorkflowXmlValidationService {

    private final WorkflowGraphBuilder graphBuilder;
    private final OsWorkflowDescriptorRegistry registry;

    public WorkflowXmlValidationReport validate(WorkflowDescriptorModel descriptor) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> unsupported = new ArrayList<>();
        List<String> risks = new ArrayList<>();

        Set<String> stepIds = new HashSet<>();
        Set<String> actionIds = new HashSet<>();
        for (WorkflowStepModel step : descriptor.getSteps()) {
            if (step.getId() == null) {
                errors.add("Step missing id: " + step.getName());
            } else if (!stepIds.add(step.getId())) {
                errors.add("Duplicate step id: " + step.getId());
            }
        }

        collectActionIssues(descriptor.getSteps(), actionIds, errors, unsupported, risks);
        collectActionList(descriptor.getCommonActions(), true, actionIds, errors, unsupported, risks);
        collectActionList(descriptor.getInitialActions(), false, actionIds, errors, unsupported, risks);

        WorkflowGraph graph = graphBuilder.build(descriptor);
        Set<String> reachable = reachability(graph);
        boolean unreachable = false;
        for (String stepId : stepIds) {
            if (!reachable.contains(stepId) && !hasInitialTo(stepId, graph)) {
                warnings.add("Unreachable step: " + stepId);
                unreachable = true;
            }
        }

        boolean deadEnds = graph.getNodesByStepId().values().stream()
                .filter(n -> !n.isTerminal())
                .anyMatch(n -> graph.getEdges().stream().noneMatch(e -> n.getStepId().equals(e.getFromStepId()))
                        && graph.getGlobalEdges().isEmpty());

        if (!unsupported.isEmpty()) {
            risks.add("Import will skip or stub " + unsupported.size() + " unsupported plugin descriptor(s)");
        }

        int transitions = graph.getEdges().size() + graph.getGlobalEdges().size() + graph.getInitialEdges().size();

        Map<String, Object> matrix = Map.of(
                "validatorsMapped", countSupported(descriptor, "validator"),
                "conditionsMapped", countSupported(descriptor, "condition"),
                "postFunctionsMapped", countSupported(descriptor, "post-function"),
                "unsupportedCount", unsupported.size()
        );

        return WorkflowXmlValidationReport.builder()
                .valid(errors.isEmpty())
                .workflowName(descriptor.getName())
                .errors(errors)
                .warnings(warnings)
                .unsupportedFeatures(unsupported)
                .executionRisks(risks)
                .stepCount(descriptor.getSteps().size())
                .transitionCount(transitions)
                .globalTransitionCount(graph.getGlobalEdges().size())
                .hasUnreachableSteps(unreachable)
                .hasDeadEnds(deadEnds)
                .graphJson(graphBuilder.toJson(graph))
                .compatibilityMatrix(matrix)
                .build();
    }

    private void collectActionIssues(List<WorkflowStepModel> steps, Set<String> actionIds,
                                     List<String> errors, List<String> unsupported, List<String> risks) {
        for (WorkflowStepModel step : steps) {
            for (WorkflowActionModel action : step.getActions()) {
                validateAction(action, step.getId(), actionIds, errors, unsupported, risks);
            }
        }
    }

    private void collectActionList(List<WorkflowActionModel> actions, boolean global, Set<String> actionIds,
                                   List<String> errors, List<String> unsupported, List<String> risks) {
        for (WorkflowActionModel action : actions) {
            validateAction(action, global ? "*" : null, actionIds, errors, unsupported, risks);
        }
    }

    private void validateAction(WorkflowActionModel action, String fromStep, Set<String> actionIds,
                                List<String> errors, List<String> unsupported, List<String> risks) {
        if (action.getId() == null) {
            errors.add("Action missing id in step " + fromStep);
        } else if (!actionIds.add(action.getId())) {
            errors.add("Duplicate action id: " + action.getId());
        }
        if (action.getResults().isEmpty()) {
            errors.add("Action " + action.getId() + " has no results");
        }
        for (WorkflowResultModel r : action.getResults()) {
            if (r.getTargetStepId() == null) {
                errors.add("Result missing step target for action " + action.getId());
            }
        }
        for (WorkflowFunctionDescriptor v : action.getValidators()) {
            unsupported.addAll(registry.unsupportedFeatures(v, "validator"));
            if ("CommentRequiredValidator".equals(classSimple(v))) {
                risks.add("Action " + action.getId() + " requires comment on transition");
            }
        }
        for (WorkflowFunctionDescriptor c : action.getConditions()) {
            unsupported.addAll(registry.unsupportedFeatures(c, "condition"));
        }
        for (WorkflowFunctionDescriptor pf : action.getPostFunctions()) {
            unsupported.addAll(registry.unsupportedFeatures(pf, "post-function"));
        }
    }

    private String classSimple(WorkflowFunctionDescriptor d) {
        if (d.getClassName() == null) {
            return "";
        }
        int i = d.getClassName().lastIndexOf('.');
        return i >= 0 ? d.getClassName().substring(i + 1) : d.getClassName();
    }

    private int countSupported(WorkflowDescriptorModel d, String category) {
        int total = 0;
        int supported = 0;
        List<WorkflowActionModel> all = new ArrayList<>();
        d.getSteps().forEach(s -> all.addAll(s.getActions()));
        all.addAll(d.getCommonActions());
        all.addAll(d.getInitialActions());
        for (WorkflowActionModel a : all) {
            List<WorkflowFunctionDescriptor> list = switch (category) {
                case "validator" -> a.getValidators();
                case "condition" -> a.getConditions();
                default -> a.getPostFunctions();
            };
            for (WorkflowFunctionDescriptor fn : list) {
                total++;
                if (registry.unsupportedFeatures(fn, category).isEmpty() && fn.getClassName() != null) {
                    supported++;
                }
            }
        }
        return supported;
    }

    private Set<String> reachability(WorkflowGraph graph) {
        Set<String> seen = new HashSet<>();
        Deque<String> q = new ArrayDeque<>();
        for (WorkflowGraph.WorkflowGraphEdge e : graph.getInitialEdges()) {
            if (e.getToStepId() != null) {
                q.add(e.getToStepId());
            }
        }
        if (q.isEmpty() && !graph.getNodesByStepId().isEmpty()) {
            q.add(graph.getNodesByStepId().keySet().iterator().next());
        }
        while (!q.isEmpty()) {
            String id = q.poll();
            if (!seen.add(id)) {
                continue;
            }
            for (WorkflowGraph.WorkflowGraphEdge e : graph.getEdges()) {
                if (id.equals(e.getFromStepId()) && e.getToStepId() != null) {
                    q.add(e.getToStepId());
                }
            }
        }
        return seen;
    }

    private boolean hasInitialTo(String stepId, WorkflowGraph graph) {
        return graph.getInitialEdges().stream().anyMatch(e -> stepId.equals(e.getToStepId()));
    }
}
