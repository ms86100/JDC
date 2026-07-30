package com.avionics_systems.test.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteTestRunRequest {
    private String status; // PASSED, FAILED, BLOCKED, SKIPPED
    private String comment;
    private String defectKeys; // comma-separated
    private List<String> stepStatuses; // Array of step results
    private List<String> evidenceLinks;
    private String logs;
    private String errorMessage;
}