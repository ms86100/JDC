package com.jira.migration.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueFieldValuesBatchRequest {
    private List<UUID> issueIds;
    private List<String> fieldKeys;
    private UUID projectId;
}
