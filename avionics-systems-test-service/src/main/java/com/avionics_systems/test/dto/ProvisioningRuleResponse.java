package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProvisioningRuleResponse {

    private UUID id;
    private UUID projectId;
    private String ruleName;
    private String description;
    private String providerType;
    private Map<String, Object> providerConfig; // Masked secrets
    private String provisioningScript;
    private Map<String, Object> capabilitiesTemplate;
    private Map<String, String> environmentTemplate;
    private Integer maxConcurrent;
    private Integer timeoutSeconds;
    private Integer retryCount;
    private Integer priority;
    private Boolean isActive;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}