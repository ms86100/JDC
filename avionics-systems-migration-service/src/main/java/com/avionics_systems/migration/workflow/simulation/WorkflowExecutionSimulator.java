package com.avionics_systems.migration.workflow.simulation;

import com.avionics_systems.migration.workflow.graph.WorkflowGraph;
import com.avionics_systems.migration.workflow.model.WorkflowActionModel;
import com.avionics_systems.migration.workflow.model.WorkflowDescriptorModel;
import com.avionics_systems.migration.workflow.registry.OsWorkflowDescriptorRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class WorkflowExecutionSimulator {

    private final OsWorkflowDescriptorRegistry registry;

    public Map<String, Object> simulate(WorkflowDescriptorModel descriptor, WorkflowGraph graph,
                                          String startStepId, List<String> transitionPath) {
        List<Map<String, Object>> trace = new ArrayList<>();
        String current = startStepId != null ? startStepId
                : graph.getInitialEdges().stream().findFirst().map(WorkflowGraph.WorkflowGraphEdge::getToStepId)
                .orElse(graph.getNodesByStepId().keySet().stream().findFirst().orElse(null));

        trace.add(step("START", current, null, List.of()));

        if (transitionPath != null) {
            for (String actionName : transitionPath) {
                WorkflowActionModel action = findAction(descriptor, current, actionName);
                if (action == null) {
                    trace.add(step("BLOCKED", current, actionName, List.of("Transition not found from step " + current)));
                    break;
                }
                List<String> conditionResults = new ArrayList<>();
                for (var c : action.getConditions()) {
                    var mapped = registry.mapCondition(c);
                    conditionResults.add(mapped.type() + (mapped.supported() ? " OK" : " UNSUPPORTED"));
                }
                List<String> validatorResults = new ArrayList<>();
                for (var v : action.getValidators()) {
                    var mapped = registry.mapValidator(v);
                    validatorResults.add(mapped.type() + (mapped.supported() ? " OK" : " UNSUPPORTED"));
                }
                String next = action.getResults().isEmpty() ? null : action.getResults().get(0).getTargetStepId();
                trace.add(step("TRANSITION", current, actionName,
                        List.of("conditions=" + conditionResults, "validators=" + validatorResults)));
                current = next;
                if (current == null) {
                    break;
                }
            }
        }

        boolean deadEnd = current != null && graph.getNodesByStepId().get(current) != null
                && graph.getNodesByStepId().get(current).isTerminal();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("finalStepId", current);
        result.put("deadEnd", deadEnd);
        result.put("trace", trace);
        result.put("pathAnalysis", Map.of(
                "stepsVisited", trace.size(),
                "requestedTransitions", transitionPath != null ? transitionPath.size() : 0
        ));
        return result;
    }

    private WorkflowActionModel findAction(WorkflowDescriptorModel d, String stepId, String actionName) {
        if (stepId == null) {
            return null;
        }
        return d.getSteps().stream()
                .filter(s -> stepId.equals(s.getId()))
                .flatMap(s -> s.getActions().stream())
                .filter(a -> actionName.equalsIgnoreCase(a.getName()))
                .findFirst()
                .orElseGet(() -> d.getCommonActions().stream()
                        .filter(a -> actionName.equalsIgnoreCase(a.getName()))
                        .findFirst().orElse(null));
    }

    private Map<String, Object> step(String phase, String stepId, String action, List<String> detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("phase", phase);
        m.put("stepId", stepId);
        m.put("action", action);
        m.put("detail", detail);
        return m;
    }
}
