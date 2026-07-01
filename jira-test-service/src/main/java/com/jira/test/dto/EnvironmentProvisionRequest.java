package com.jira.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvironmentProvisionRequest {

    private UUID matrixId;

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    private UUID combinationId;

    private UUID provisioningRuleId; // Optional - auto-select if not provided

    private UUID testExecutionId; // Optional - link to execution

    private UUID requestedBy;
}