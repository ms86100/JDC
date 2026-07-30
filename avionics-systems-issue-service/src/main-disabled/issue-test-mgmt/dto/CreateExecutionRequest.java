package com.avionics_systems.issue.dto;

import lombok.*;
import java.util.UUID;

/**
 * Request DTO for creating a test execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExecutionRequest {

    private UUID projectId;
    private UUID testPlanId;
    private UUID testSetId;
    private UUID testId;
    private String name;
    private String description;
    private String testEnv; // DEV, STAGING, PROD
    private String testCycle;
    private UUID sprintId;
    private String ciBuildUrl;
    private String ciJobName;
    private String ciBuildNumber;
    private String ciJobId;
    private String branch;
    private String commitSha;
}