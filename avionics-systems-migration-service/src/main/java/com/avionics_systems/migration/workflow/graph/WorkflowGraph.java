package com.avionics_systems.migration.workflow.graph;

import lombok.*;

import java.util.*;

@Data
@Builder
public class WorkflowGraph {
    private String workflowName;
    @Builder.Default
    private Map<String, WorkflowGraphNode> nodesByStepId = new LinkedHashMap<>();
    @Builder.Default
    private List<WorkflowGraphEdge> edges = new ArrayList<>();
    @Builder.Default
    private List<WorkflowGraphEdge> globalEdges = new ArrayList<>();
    @Builder.Default
    private List<WorkflowGraphEdge> initialEdges = new ArrayList<>();

    @Data
    @Builder
    public static class WorkflowGraphNode {
        private String stepId;
        private String stepName;
        private String statusId;
        private String statusName;
        private boolean terminal;
    }

    @Data
    @Builder
    public static class WorkflowGraphEdge {
        private String actionId;
        private String actionName;
        private String fromStepId;
        private String toStepId;
        private String osWorkflowStatus;
        private boolean global;
        private boolean initial;
        private boolean conditional;
        private String view;
    }
}
