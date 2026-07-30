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
public class EpicBurndownResponse {
    private UUID epicId;
    private String epicName;
    private List<EpicSprintEntry> sprintEntries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EpicSprintEntry {
        private UUID sprintId;
        private String sprintName;
        private int totalPoints;
        private int completedPoints;
        private int remainingPoints;
    }
}
