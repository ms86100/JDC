package com.avionics_systems.issue.dto;

import lombok.*;

/**
 * CI/CD execution update DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CiExecutionUpdate {

    private Integer totalTests;
    private Integer passedTests;
    private Integer failedTests;
    private Integer blockedTests;
    private Integer skippedTests;
    private String status;
    private String testReportUrl;
}