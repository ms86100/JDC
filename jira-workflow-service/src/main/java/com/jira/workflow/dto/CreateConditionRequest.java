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

    @NotNull(message = "{validation.transition.id.required}")
    private UUID transitionId;

    @NotBlank(message = "{validation.condition.type.required}")
    private String conditionType;

    private String fieldName;
    private String operator;
    private String value;
    private String conditionData;
    private Boolean negate;
    private Integer sequence;
}