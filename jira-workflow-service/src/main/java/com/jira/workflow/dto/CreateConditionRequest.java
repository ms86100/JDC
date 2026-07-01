package com.jira.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConditionRequest {

    @NotNull(message = "Transition ID is required")
    private UUID transitionId;

    @NotBlank(message = "Condition type is required")
    private String conditionType;

    private String fieldName;
    private String operator;
    private String value;
    private String conditionData;
    private Boolean negate;
    private Integer sequence;
}