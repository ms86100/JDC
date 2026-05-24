package com.jira.dashboard.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GadgetInstanceResponse {

    private UUID id;
    private UUID dashboardId;
    private UUID gadgetId;
    private String gadgetModuleKey;
    private String gadgetCategory;
    private String title;
    private Integer positionRow;
    private Integer positionColumn;
    private Integer width;
    private Integer height;
    private String config;
    private String filters;
    private String color;
    private Boolean isMinimized;
    private Boolean isCollapsed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}