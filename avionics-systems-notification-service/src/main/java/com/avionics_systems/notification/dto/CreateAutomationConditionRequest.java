package com.avionics_systems.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAutomationConditionRequest {

    @NotBlank(message = "{validation.automation.condition.type.required}")
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