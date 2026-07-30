package com.avionics_systems.workflow.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateTransitionResponse {

    private UUID transitionId;
    private UUID fromStatusId;
    private UUID toStatusId;
    private boolean valid;
    private String message;
    private List<String> errors;
    private List<String> warnings;
}