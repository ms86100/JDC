package com.avionics_systems.issue.dto;

import lombok.*;
import java.util.UUID;

/**
 * CI/CD execution trigger request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CiExecutionRequest {

    private UUID testSetId;
    private UUID testPlanId;
    private String name;
    private String description;
    private String testEnv;
    private String testCycle;
    private String ciBuildUrl;
    private String ciJobName;
    private String ciBuildNumber;
    private String ciJobId;
    private String branch;
    private String commitSha;
}