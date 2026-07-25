package com.jira.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAutomationRuleRequest {

    @NotBlank(message = "{validation.automation.rule.name.required}")
    private String name;

    private String description;

    private UUID projectId;

    @NotBlank(message = "{validation.automation.rule.triggerType.required}")
    private String triggerType;

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private Boolean isSystemRule = false;

    @Builder.Default
    private Integer orderIndex = 0;
}