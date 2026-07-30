package com.avionics_systems.sprint.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgileBoardResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID projectId;
    private String boardType;
    private UUID filterId;
    private String jqlQuery;
    private Boolean isDefault;
    private Boolean allowAllIssues;
    private Boolean isCommunity;
    private String location;
    private Boolean canManage;
    private String columnConfig;
    private String cardLayout;
    private String estimationStatistic;
    private Integer daysOnBoard;
    private LocalDateTime lastViewed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}