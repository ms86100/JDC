package com.avionics_systems.workflow.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitionValidationResponse {

    private UUID transitionId;
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;
}