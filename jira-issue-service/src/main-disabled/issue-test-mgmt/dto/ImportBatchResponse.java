package com.jira.issue.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for import batch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportBatchResponse {

    private UUID id;
    private String importType; // JUNIT, CUCUMBER, TESTNG
    private String ciSource; // JENKINS, GITHUB_ACTIONS, GITLAB_CI, BAMBOO
    private String ciBuildUrl;
    private String status; // QUEUED, PROCESSING, COMPLETED, FAILED, PARTIAL
    private Integer totalTests;
    private Integer totalPassed;
    private Integer totalFailed;
    private Integer totalSkipped;
    private Integer testsCreated;
    private Integer testsUpdated;
    private Integer executionsCreated;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}