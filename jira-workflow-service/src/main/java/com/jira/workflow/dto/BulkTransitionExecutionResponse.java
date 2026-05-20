package com.jira.workflow.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkTransitionExecutionResponse {
    private int total;
    private int succeeded;
    private int failed;
    private List<BulkTransitionResultItem> results;
}
