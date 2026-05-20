package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProvisionResponse {

    private UUID combinationId;
    private UUID provisioningRuleId;
    private String providerType;
    private Map<String, Object> provisionedConfig;
    private Map<String, String> environmentVariables;
    private String accessUrl;
    private String credentials; // Masked
    private LocalDateTime expiresAt;
    private LocalDateTime provisionedAt;
    private String status;
    private String errorMessage;
}