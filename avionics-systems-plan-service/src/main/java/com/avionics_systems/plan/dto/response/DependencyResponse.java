package com.avionics_systems.plan.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependencyResponse {

    private UUID id;
    private UUID planId;
    private UUID blockingIssueId;
    private String blockingIssueKey;
    private String blockingIssueSummary;
    private String blockingIssueStatus;
    private UUID blockedIssueId;
    private String blockedIssueKey;
    private String blockedIssueSummary;
    private String blockedIssueStatus;
    private String dependencyType;
    private Boolean isCircular;
    private String blockingPath;
    private LocalDateTime createdAt;
}
