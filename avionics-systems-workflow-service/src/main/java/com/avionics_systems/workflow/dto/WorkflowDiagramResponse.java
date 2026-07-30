package com.avionics_systems.workflow.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

/**
 * DTO for workflow diagram visualization response.
 * F3-US008: Workflow Visualization
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDiagramResponse {

    private UUID workflowId;
    private UUID layoutId;
    private Integer version;
    private Boolean isLocked;
    private UUID lockedBy;
    private List<DiagramNode> nodes;
    private List<DiagramEdge> edges;
    private DiagramMetadata metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiagramNode {
        private UUID id;
        private UUID statusId;
        private String statusName;
        private String nodeType;
        private Double positionX;
        private Double positionY;
        private Double width;
        private Double height;
        private String label;
        private String color;
        private Boolean isExpanded;
        private String iconUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiagramEdge {
        private UUID id;
        private UUID transitionId;
        private UUID fromNodeId;
        private UUID toNodeId;
        private String edgeType;
        private String pathPoints;
        private Double labelOffsetX;
        private Double labelOffsetY;
        private Boolean isLooped;
        private String label;
        private Boolean hasCondition;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiagramMetadata {
        private Integer totalNodes;
        private Integer totalEdges;
        private String layoutData;
        private Boolean isMinimap;
        private String exportedAt;
        private String format;
    }
}