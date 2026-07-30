package com.avionics_systems.issue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * GraphQL Input Types for Test Management
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestIssueInput {
    private UUID projectId;
    private String title;
    private String description;
    private String testType;
    private String testStatus;
    private String testPriority;
    private UUID testOwnerId;
    private List<TestStepInput> testSteps;
    private List<String> requirementKeys;
    private List<String> labels;
    private UUID assigneeId;
    private UUID folderId;
}