package com.jira.workflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSchemeMappingRequest {
    @NotNull(message = "{validation.issuetype.required}")
    private UUID issueTypeId;

    @NotNull(message = "{validation.workflow.id.required}")
    private UUID workflowId;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class BulkMappingRequest {
    private java.util.List<WorkflowSchemeMappingRequest> mappings;
}