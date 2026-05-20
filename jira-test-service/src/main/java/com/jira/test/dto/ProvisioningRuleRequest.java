package com.jira.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProvisioningRuleRequest {

    private UUID projectId;

    @NotNull(message = "Rule name is required")
    private String ruleName;

    private String description;

    @NotNull(message = "Provider type is required")
    private String providerType; // BROWSERSTACK, SAUCELABS, KUBERNETES, DOCKER, LOCAL

    private Map<String, Object> providerConfig;

    private String provisioningScript;

    private Map<String, Object> capabilitiesTemplate;

    private Map<String, String> environmentTemplate;

    private Integer maxConcurrent;

    private Integer timeoutSeconds;

    private Integer retryCount;

    private Integer priority;

    private UUID createdBy;
}