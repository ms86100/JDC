package com.jira.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemReportSummaryReport {
    private UUID projectId;
    private int totalReports;
    private int openCount;
    private Map<String, Long> countByStatus;
    private Map<String, Long> countByPrType;
    private Map<String, Long> countByPrOrigin;
    private LocalDateTime generatedAt;
}
