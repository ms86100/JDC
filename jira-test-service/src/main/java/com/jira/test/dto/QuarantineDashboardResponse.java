package com.jira.test.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineDashboardResponse {

    private Integer totalQuarantined;
    private Integer quarantinedCount;
    private Integer investigationCount;
    private Integer candidateCount;
    private Integer restoredThisWeek;
    private BigDecimal averageQuarantineDurationDays;
    private Map<String, Integer> byTriggerType;
    private List<QuarantineResponse> recentQuarantined;
    private List<QuarantineResponse> readyForRestore;
}