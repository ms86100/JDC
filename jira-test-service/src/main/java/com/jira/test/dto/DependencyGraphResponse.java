package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DependencyGraphResponse {

    private UUID sharedStepId;
    private String sharedStepName;
    private Integer currentVersion;

    // Graph structure
    private GraphNode root;
    private List<GraphNode> allNodes;
    private List<GraphEdge> allEdges;

    // Statistics
    private Integer totalDependencies;
    private Integer directDependencies;
    private Integer transitiveDependencies;
    private Integer dependentSteps; // Steps that depend on this one
    private Integer maxDepth;

    // Analysis
    private Boolean hasCircularDependency;
    private List<String> circularPaths;
    private List<String> criticalPaths;

    // Summary
    private String complexityLevel; // LOW, MEDIUM, HIGH
    private Double maintainabilityScore;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GraphNode {
        private UUID id;
        private String name;
        private Integer version;
        private String nodeType; // SHARED_STEP, EXTERNAL_STEP
        private Integer depth;
        private Boolean isRoot;
        private Integer usageCount;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GraphEdge {
        private UUID sourceId;
        private UUID targetId;
        private String edgeType; // DEPENDS_ON, COMPOSED_OF, REFERENCES
        private Integer weight; // 1-10, indicating strength of relationship
    }
}