package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDefinitionDto {

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

    // Parsed workflow data
    private String initialState;
    private List<String> states;
    private List<String> finalStates;
}