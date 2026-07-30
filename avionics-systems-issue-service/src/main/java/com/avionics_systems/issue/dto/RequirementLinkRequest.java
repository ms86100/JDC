package com.avionics_systems.issue.dto;

import lombok.*;
import java.util.UUID;

/**
 * Request DTO for linking requirement to test
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementLinkRequest {

    private String requirementKey;
    private String requirementSummary;
    private String requirementType; // EPIC, STORY, REQUIREMENT, BUG
    private UUID testIssueId;
    private String coverageStatus; // COVERED, PARTIAL, NOT_COVERED
}