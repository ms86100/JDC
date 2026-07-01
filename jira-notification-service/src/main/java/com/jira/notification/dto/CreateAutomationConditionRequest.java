package com.jira.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAutomationConditionRequest {

    @NotBlank(message = "Condition type is required")
    private String conditionType;

    private String fieldName;
    private String operator;
    private String conditionValue;
    private String conditionConfig;

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private String logicalGroup = "ALL";

    @Builder.Default
    private Integer orderIndex = 0;
}