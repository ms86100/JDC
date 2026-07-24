package com.jira.workflow.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationRuleResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID projectId;
    private Boolean isEnabled;

    // Trigger
    private String triggerType;
    private String triggerConfig;

    // Conditions
    private String conditions;

    // Actions
    private String actions;

    // Branch
    private String branchType;
    private String branchLinkType;
    private String branchActions;

    // Execution stats
    private Integer executionCount;
    private LocalDateTime lastExecutedAt;
    private String lastError;

    // Audit
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
