package com.jira.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepImpactResponse {

    private UUID testId;
    private String testIssueKey;
    private String testName;
    private Integer usageCount;
    private String lastUsedAt;
    private String status;
}