package com.avionics_systems.migration.workflow.graph;

import com.avionics_systems.migration.workflow.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class WorkflowGraphBuilder {

    public WorkflowGraph build(WorkflowDescriptorModel descriptor) {
        Map<String, WorkflowGraph.WorkflowGraphNode> nodes = new LinkedHashMap<>();
        Set<String> stepsWithOutbound = new HashSet<>();
        List<WorkflowGraph.WorkflowGraphEdge> edges = new ArrayList<>();
        List<WorkflowGraph.WorkflowGraphEdge> globalEdges = new ArrayList<>();
        List<WorkflowGraph.WorkflowGraphEdge> initialEdges = new ArrayList<>();

        for (WorkflowStepModel step : descriptor.getSteps()) {
            String statusName = step.getMeta().getOrDefault("legacy.status.name", step.getName());
            String statusId = step.getMeta().getOrDefault("legacy.status.id", step.getId());
            nodes.put(step.getId(), WorkflowGraph.WorkflowGraphNode.builder()
                    .stepId(step.getId())
                    .stepName(step.getName())
                    .statusId(statusId)
                    .statusName(statusName)
                    .terminal(step.getActions() == null || step.getActions().isEmpty())
                    .build());
        }

        for (WorkflowStepModel step : descriptor.getSteps()) {
            for (WorkflowActionModel action : step.getActions()) {
                addActionEdges(action, step.getId(), edges, stepsWithOutbound, false, false);
            }
        }
        for (WorkflowActionModel action : descriptor.getCommonActions()) {
            addGlobalEdges(action, nodes.keySet(), globalEdges, stepsWithOutbound);
        }
        for (WorkflowActionModel action : descriptor.getInitialActions()) {
            addActionEdges(action, null, initialEdges, stepsWithOutbound, true, false);
        }

        for (WorkflowGraph.WorkflowGraphNode node : nodes.values()) {
            if (stepsWithOutbound.contains(node.getStepId())) {
                node.setTerminal(false);
            }
        }

        return WorkflowGraph.builder()
                .workflowName(descriptor.getName())
                .nodesByStepId(nodes)
                .edges(edges)
                .globalEdges(globalEdges)
                .initialEdges(initialEdges)
                .build();
    }

    private void addGlobalEdges(WorkflowActionModel action, Set<String> stepIds,
                                List<WorkflowGraph.WorkflowGraphEdge> globalEdges, Set<String> outbound) {
        for (WorkflowResultModel result : action.getResults()) {
            String to = result.getTargetStepId();
            if (to == null) {
                continue;
            }
            for (String from : stepIds) {
                globalEdges.add(edge(action, from, to, result, true, false));
                outbound.add(from);
            }
        }
    }

    private void addActionEdges(WorkflowActionModel action, String fromStepId,
                                List<WorkflowGraph.WorkflowGraphEdge> target,
                                Set<String> outbound, boolean initial, boolean global) {
        for (WorkflowResultModel result : action.getResults()) {
            String to = result.getTargetStepId();
            if (to == null) {
                continue;
            }
            target.add(edge(action, fromStepId, to, result, global, initial));
            if (fromStepId != null) {
                outbound.add(fromStepId);
            }
        }
    }

    private WorkflowGraph.WorkflowGraphEdge edge(WorkflowActionModel action, String from, String to,
                                                 WorkflowResultModel result, boolean global, boolean initial) {
        return WorkflowGraph.WorkflowGraphEdge.builder()
                .actionId(action.getId())
                .actionName(action.getName())
                .fromStepId(from)
                .toStepId(to)
                .osWorkflowStatus(result.getStatus())
                .global(global)
                .initial(initial)
                .conditional("result".equals(result.getType()) && !result.getConditions().isEmpty())
                .view(action.getView())
                .build();
    }

    public Map<String, Object> toJson(WorkflowGraph graph) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("workflowName", graph.getWorkflowName());
        json.put("nodes", graph.getNodesByStepId().values());
        json.put("edges", graph.getEdges());
        json.put("globalEdges", graph.getGlobalEdges());
        json.put("initialEdges", graph.getInitialEdges());
        return json;
    }
}
