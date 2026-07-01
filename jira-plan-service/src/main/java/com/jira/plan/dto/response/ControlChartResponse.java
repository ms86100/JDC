package com.jira.plan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControlChartResponse {
    private UUID boardId;
    private double averageCycleTime;
    private double averageLeadTime;
    private double standardDeviation;
    private List<IssueTimingEntry> issues;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueTimingEntry {
        private UUID issueId;
        private UUID planItemId;
        private double cycleTimeDays;
        private double leadTimeDays;
        private LocalDateTime completedAt;
    }
}
