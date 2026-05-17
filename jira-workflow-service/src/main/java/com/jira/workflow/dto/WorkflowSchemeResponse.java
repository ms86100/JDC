package com.jira.workflow.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSchemeResponse {
    private UUID id;
    private String name;
    private String description;
    private Boolean isDefault;
    private UUID defaultWorkflowId;
    private Boolean isDraft;
    private UUID draftOfSchemeId;
    private Boolean isActive;
    private List<WorkflowSchemeMappingResponse> mappings;
    private Integer issueTypeCount;
    private Integer projectCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class WorkflowSchemeMappingResponse {
    private UUID id;
    private UUID issueTypeId;
    private UUID workflowId;
    private String workflowName;
    private LocalDateTime createdAt;
}