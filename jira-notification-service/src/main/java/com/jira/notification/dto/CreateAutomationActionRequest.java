package com.jira.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAutomationActionRequest {

    @NotBlank(message = "{validation.automation.action.type.required}")
    private String actionType;

    private String actionConfig;

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private Integer orderIndex = 0;

    @Builder.Default
    private String failureHandling = "CONTINUE";
}