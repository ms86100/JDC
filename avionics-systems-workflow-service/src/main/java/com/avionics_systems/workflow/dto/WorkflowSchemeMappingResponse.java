package com.avionics_systems.workflow.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSchemeMappingResponse {
    private UUID id;
    private UUID issueTypeId;
    private UUID workflowId;
    private String workflowName;
    private LocalDateTime createdAt;
}