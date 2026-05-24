package com.jira.notification.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationConditionResponse {

    private UUID id;
    private UUID ruleId;
    private String conditionType;
    private String fieldName;
    private String operator;
    private String conditionValue;
    private String conditionConfig;
    private Boolean enabled;
    private String logicalGroup;
    private Integer orderIndex;
    private OffsetDateTime createdAt;
}