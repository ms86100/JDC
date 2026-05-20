package com.jira.issue.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIssueStatusRequest {

    /** Target status (used when transitionId omitted — engine resolves transition) */
    private UUID statusId;

    /** Preferred: explicit workflow transition */
    private UUID transitionId;

    private String comment;
    private UUID resolutionId;
    private java.util.Map<String, Object> screenInput;
}