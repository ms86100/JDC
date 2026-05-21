package com.jira.issue.dto.bulk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkOperationResultItem {
    private UUID issueId;
    private String issueKey;
    private boolean success;
    private String message;
    private String errorCode;
}
