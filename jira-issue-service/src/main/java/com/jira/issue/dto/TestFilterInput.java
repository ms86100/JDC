package com.jira.issue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestFilterInput {
    private UUID projectId;
    private String testType;
    private String testStatus;
    private String testPriority;
    private List<String> labels;
    private String requirementKey;
    private UUID assigneeId;
    private UUID testSetId;
    private UUID testPlanId;
    private String search;
}