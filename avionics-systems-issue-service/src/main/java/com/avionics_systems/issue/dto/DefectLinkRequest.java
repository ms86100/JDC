package com.avionics_systems.issue.dto;

import lombok.*;
import java.util.UUID;

/**
 * Request DTO for linking defect to test execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefectLinkRequest {

    private String defectKey;
    private String defectSummary;
    private String defectType;
    private UUID testExecutionId;
    private UUID stepResultId;
    private UUID testIssueId;
    private String severity; // CRITICAL, MAJOR, MINOR
    private String priority; // P1, P2, P3, P4
}