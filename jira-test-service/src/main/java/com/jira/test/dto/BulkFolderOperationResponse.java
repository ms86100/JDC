package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkFolderOperationResponse {

    private Integer totalRequested;
    private Integer successCount;
    private Integer failedCount;
    private String status;

    private List<FolderOperationResult> results;
}