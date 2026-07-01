package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderAnalyticsResponse {

    private UUID folderId;
    private String folderName;
    private UUID projectId;
    private String path;

    private Integer totalTests;
    private Integer directTests;
    private Integer childFolderCount;

    private Integer passedTests;
    private Integer failedTests;
    private Integer blockedTests;
    private Integer notRunTests;
    private Integer flakyTests;

    private Double passRate;
    private Double executionProgress;
    private Double flakyRate;

    private LocalDateTime lastExecutionDate;
    private LocalDateTime lastModifiedDate;

    private Map<String, Integer> testsByPriority;
    private Map<String, Integer> testsByComponent;
    private Map<String, Integer> executionsByDay;

    private Double healthScore;
    private String healthStatus;
    private List<String> issues;
}