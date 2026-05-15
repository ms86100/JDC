package com.jira.issue.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueSearchRequest {

    private UUID projectId;
    private UUID status;
    private UUID assigneeId;
    private UUID reporterId;
    private UUID priorityId;
    private UUID issueTypeId;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}