package com.jira.workflow.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowLayoutEdgeResponse {
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