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
public class BenchDefectSummaryReport {
    private UUID projectId;
    private int totalDefects;
    private Map<String, Long> countByStatus;
    private Map<String, Long> countBySeverity;
    private LocalDateTime generatedAt;
}
