package com.jira.issue.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    /** Set from path variable on POST /api/issues/{issueId}/links */
    private UUID sourceIssueId;

    @NotNull(message = "Target issue ID is required")
    @JsonAlias("destinationIssueId")
    private UUID targetIssueId;

    private UUID linkTypeId;

    /** Link type name e.g. blocks, relates to (resolved to linkTypeId when id omitted) */
    @JsonAlias("linkType")
    private String linkTypeName;
}