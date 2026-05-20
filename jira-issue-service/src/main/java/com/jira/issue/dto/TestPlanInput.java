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
public class TestPlanInput {
    private UUID projectId;
    private String name;
    private String description;
    private String testCycle;
    private String testEnvironment;
    private List<UUID> testSetIds;
    private List<UUID> testIds;
}