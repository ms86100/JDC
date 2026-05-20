package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportBatchResponse {
    private UUID id;
    private String importType;
    private String ciSource;
    private String ciBuildUrl;
    private String ciJobName;
    private String ciBuildNumber;
    private int totalTests;
    private int totalPassed;
    private int totalFailed;
    private int totalSkipped;
    private String status;
    private String errorMessage;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime finishedAt;
}