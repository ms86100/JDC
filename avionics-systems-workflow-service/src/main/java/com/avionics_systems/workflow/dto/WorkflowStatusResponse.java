package com.avionics_systems.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStatusResponse {

    private UUID id;
    private UUID workflowId;
    private UUID statusId;
    private String statusName;
    private String statusCategory;
    private String statusColor;
    private Integer sequence;
    private LocalDateTime createdAt;
}