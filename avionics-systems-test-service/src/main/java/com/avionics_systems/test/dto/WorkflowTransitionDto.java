package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTransitionDto {

    private UUID id;
    private UUID instanceId;
    private String fromState;
    private String toState;
    private UUID transitionedBy;
    private LocalDateTime transitionedAt;
    private String comment;
    private String transitionType;
}