package com.avionics_systems.workflow.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionResponse {

    private UUID id;
    private UUID transitionId;
    private String conditionType;
    private String fieldName;
    private String operator;
    private String value;
    private String conditionData;
    private Boolean negate;
    private Integer sequence;
    private LocalDateTime createdAt;
}