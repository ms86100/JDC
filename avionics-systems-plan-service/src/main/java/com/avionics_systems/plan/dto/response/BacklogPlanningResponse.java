package com.avionics_systems.plan.dto.response;

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
public class BacklogPlanningResponse {
    private UUID boardId;
    private List<SprintBacklogSection> sprintSections;
    private BacklogSection backlog;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SprintBacklogSection {
        private UUID sprintId;
        private String sprintName;
        private String sprintState;
        private int totalIssues;
        private int totalPoints;
        private List<SprintIssueResponse> issues;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BacklogSection {
        private int totalIssues;
        private int totalPoints;
        private List<UUID> planItemIds;
    }
}
