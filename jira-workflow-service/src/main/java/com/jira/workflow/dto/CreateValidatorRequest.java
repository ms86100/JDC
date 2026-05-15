package com.jira.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateValidatorRequest {

    @NotNull(message = "Transition ID is required")
    private UUID transitionId;

    @NotBlank(message = "Validator type is required")
    private String validatorType;

    private String fieldName;
    private String validatorData;
    private String errorMessage;
    private Integer sequence;
    private Boolean continueOnError;
}