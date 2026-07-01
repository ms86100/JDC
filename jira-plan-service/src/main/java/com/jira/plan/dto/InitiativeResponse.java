package com.jira.plan.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiativeResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;
    private UUID programId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate targetDate;
    private Integer totalStoryPoints;
    private Integer completedStoryPoints;
    private Double progressPercentage;
    private String color;
    private String avatarUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<EpicProgress> epics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EpicProgress {
        private UUID epicId;
        private String epicKey;
        private String epicName;
        private Integer totalStoryPoints;
        private Integer completedStoryPoints;
        private Double progressPercentage;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
    }
}