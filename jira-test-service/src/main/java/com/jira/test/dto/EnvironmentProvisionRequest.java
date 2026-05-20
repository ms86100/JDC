package com.jira.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvironmentProvisionRequest {

    private UUID matrixId;

    private UUID combinationId;

    private UUID provisioningRuleId; // Optional - auto-select if not provided

    private UUID testExecutionId; // Optional - link to execution

    private UUID requestedBy;
}