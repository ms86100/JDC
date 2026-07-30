package com.avionics_systems.workflow.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowLayoutNodeResponse {
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