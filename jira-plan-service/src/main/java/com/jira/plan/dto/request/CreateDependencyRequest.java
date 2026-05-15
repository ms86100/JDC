package com.jira.plan.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDependencyRequest {

    @NotBlank(message = "Blocking issue ID is required")
    private UUID blockingIssueId;

    private String blockingIssueKey;

    @NotBlank(message = "Blocked issue ID is required")
    private UUID blockedIssueId;

    private String blockedIssueKey;

    private String dependencyType;
}