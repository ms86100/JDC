package com.jira.issue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueLinkRequest {
    @NotNull(message = "Source issue ID is required")
    private UUID sourceIssueId;

    @NotNull(message = "Target issue ID is required")
    private UUID targetIssueId;

    @NotBlank(message = "Link type ID is required")
    private UUID linkTypeId;

    private String linkTypeName;
}