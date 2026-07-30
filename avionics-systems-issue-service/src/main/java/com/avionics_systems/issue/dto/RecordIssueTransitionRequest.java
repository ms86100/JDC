package com.avionics_systems.issue.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordIssueTransitionRequest {

    private UUID projectId;
    private UUID workflowId;
    private UUID transitionId;
    private String transitionName;
    private UUID fromStatusId;
    private UUID toStatusId;
    private UUID userId;
    private String comment;
    private Boolean success;
    private String errorMessage;
}
