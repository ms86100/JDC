package com.jira.workflow.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowLayoutResponse {
    private UUID id;
    private UUID workflowId;
    private String layoutData;
    private Integer layoutVersion;
    private Boolean isLocked;
    private UUID lockedBy;
    private LocalDateTime lockedAt;
    private List<WorkflowLayoutNodeResponse> nodes;
    private List<WorkflowLayoutEdgeResponse> edges;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class WorkflowLayoutNodeResponse {
    private UUID id;
    private UUID statusId;
    private String statusName;
    private String nodeType;
    private Double positionX;
    private Double positionY;
    private Double width;
    private Double height;
    private String color;
    private Boolean isExpanded;
    private String label;
    private Integer sortOrder;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class WorkflowLayoutEdgeResponse {
    private UUID id;
    private UUID transitionId;
    private UUID fromNodeId;
    private UUID toNodeId;
    private String edgeType;
    private String pathPoints;
    private Double labelOffsetX;
    private Double labelOffsetY;
    private Boolean isLooped;
    private Integer sortOrder;
}