package com.avionics_systems.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateValidatorRequest {

    @NotNull(message = "{validation.transition.id.required}")
    private UUID transitionId;

    @NotBlank(message = "{validation.validator.type.required}")
    private String validatorType;

    private String fieldName;
    private String validatorData;
    private String errorMessage;
    private Integer sequence;
    private Boolean continueOnError;
}