package com.avionics_systems.workflow.dto;

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