package com.jira.test.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkOperationResponse {

    private int successCount;
    private int failedCount;
    private List<String> errors;
}
