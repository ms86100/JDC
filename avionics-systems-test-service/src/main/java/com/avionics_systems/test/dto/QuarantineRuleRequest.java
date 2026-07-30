package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineRuleRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotBlank(message = "Rule name is required")
    private String ruleName;

    @NotBlank(message = "Rule type is required")
    private String ruleType; // flaky_threshold, failure_streak, environment

    @NotNull(message = "Conditions are required")
    private Map<String, Object> conditions; // {flakyScore: 0.7, consecutiveFails: 10}

    private Boolean autoQuarantine;

    private Boolean notifyOnTrigger;

    private Boolean isActive;
}