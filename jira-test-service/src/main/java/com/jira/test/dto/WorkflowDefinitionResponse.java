package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDefinitionResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID projectId;
    private String workflowType;
    private String workflowStepsJson;
    private String transitionRulesJson;
    private Boolean isDefault;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}