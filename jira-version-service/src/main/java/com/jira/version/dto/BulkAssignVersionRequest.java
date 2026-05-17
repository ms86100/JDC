package com.jira.version.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkAssignVersionRequest {
    private List<UUID> issueIds;
    private UUID versionId;
    private UUID targetVersionId; // For move operation
}