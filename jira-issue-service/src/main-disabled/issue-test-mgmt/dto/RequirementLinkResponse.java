package com.jira.issue.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for requirement link
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementLinkResponse {

    private UUID id;
    private String requirementKey;
    private String requirementSummary;
    private String requirementType;
    private UUID testIssueId;
    private String coverageStatus;
    private UUID lastTestExecutionId;
    private String lastExecutionStatus;
    private LocalDateTime createdAt;
}