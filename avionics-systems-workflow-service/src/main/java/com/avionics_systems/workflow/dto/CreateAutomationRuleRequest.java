package com.avionics_systems.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAutomationRuleRequest {

    @NotBlank(message = "{validation.rule.name.required}")
    private String name;

    private String description;

    private UUID projectId;

    @NotBlank(message = "{validation.rule.trigger.type.required}")
    private String triggerType;

    private String triggerConfig;

    private String conditions;

    @NotNull(message = "{validation.rule.actions.required}")
    private String actions;

    private String branchType;

    private String branchLinkType;

    private String branchActions;
}
