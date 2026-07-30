package com.avionics_systems.issue.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for test execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestExecutionResponse {

    private UUID id;
    private UUID projectId;
    private UUID testPlanId;
    private UUID testSetId;
    private UUID testId;
    private String name;
    private String description;
    private String status;
    private String testEnv;
    private UUID testerId;
    private String testCycle;
    private Integer totalTests;
    private Integer passedTests;
    private Integer failedTests;
    private Integer blockedTests;
    private Integer skippedTests;
    private Integer notRunTests;
    private Double passRate;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long durationSeconds;
    private String ciBuildUrl;
    private String ciJobName;
    private String ciBuildNumber;
    private String branch;
    private String commitSha;
    private LocalDateTime createdAt;
}