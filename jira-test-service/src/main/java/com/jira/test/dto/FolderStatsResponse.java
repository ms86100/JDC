package com.jira.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderStatsResponse {

    private UUID folderId;
    private String folderName;
    private UUID projectId;

    private Integer totalTests;
    private Integer directTests;

    private Integer passedTests;
    private Integer failedTests;
    private Integer blockedTests;
    private Integer notRunTests;

    private Double passRate;
    private Double executionProgress;
}