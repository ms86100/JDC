package com.jira.plan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkMoveIssuesResponse {
    private int addedCount;
    private int removedFromPreviousCount;
    private List<SprintIssueResponse> movedIssues;
}
