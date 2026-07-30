package com.avionics_systems.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitionExecutionResponse {

    private boolean success;

    private String error;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    @Builder.Default
    private Map<String, String> validationErrors = new LinkedHashMap<>();

    private UUID newStatusId;

    private UUID transitionId;

    private UUID issueId;

    private Long executionTimeMs;
}