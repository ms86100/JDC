package com.avionics_systems.workflow.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidatorResponse {

    private UUID id;
    private UUID transitionId;
    private String validatorType;
    private String fieldName;
    private String validatorData;
    private String errorMessage;
    private Integer sequence;
    private Boolean continueOnError;
    private LocalDateTime createdAt;
}