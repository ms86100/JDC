package com.avionics_systems.workflow.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostFunctionResponse {

    private UUID id;
    private UUID transitionId;
    private String functionType;
    private String functionData;
    private Integer sequence;
    private Boolean async;
    private Boolean failOnError;
    private LocalDateTime createdAt;
}