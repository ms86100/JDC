package com.jira.version.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignVersionToIssueRequest {

    @NotNull(message = "{validation.version.issueId.required}")
    private UUID issueId;

    private UUID versionId;

    // For bulk operations
    private java.util.List<UUID> issueIds;
}