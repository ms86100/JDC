package com.jira.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAutomationTriggerRequest {

    @NotBlank(message = "Trigger type is required")
    private String triggerType;

    private String triggerConfig;

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private Integer orderIndex = 0;
}