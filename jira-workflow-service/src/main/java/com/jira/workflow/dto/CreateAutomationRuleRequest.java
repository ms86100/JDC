package com.jira.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAutomationRuleRequest {

    @NotBlank(message = "Rule name is required")
    private String name;

    private String description;

    private UUID projectId;

    @NotBlank(message = "Trigger type is required")
    private String triggerType;

    private String triggerConfig;

    private String conditions;

    @NotNull(message = "Actions are required")
    private String actions;

    private String branchType;

    private String branchLinkType;

    private String branchActions;
}
