package com.jira.plan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VelocityChartResponse {
    private UUID boardId;
    private double averageVelocity;
    private List<SprintVelocityEntry> sprints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SprintVelocityEntry {
        private UUID sprintId;
        private String sprintName;
        private LocalDate startDate;
        private LocalDate endDate;
        private int committedPoints;
        private int completedPoints;
    }
}
