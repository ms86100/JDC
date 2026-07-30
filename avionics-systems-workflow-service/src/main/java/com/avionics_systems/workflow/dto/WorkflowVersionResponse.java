package com.avionics_systems.workflow.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowVersionResponse {
    private UUID id;
    private UUID workflowId;
    private Integer versionNumber;
    private String workflowSnapshot;
    private String statusesSnapshot;
    private String transitionsSnapshot;
    private LocalDateTime createdAt;
    private UUID createdBy;
    private String changeDescription;
    private String changeType;
}