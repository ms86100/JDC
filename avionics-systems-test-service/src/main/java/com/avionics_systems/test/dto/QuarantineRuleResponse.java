package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineRuleResponse {

    private UUID id;
    private UUID projectId;
    private String ruleName;
    private String ruleType;
    private Map<String, Object> conditions;
    private Boolean autoQuarantine;
    private Boolean notifyOnTrigger;
    private Boolean isActive;
    private UUID createdBy;
    private LocalDateTime createdAt;
}