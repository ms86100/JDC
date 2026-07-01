package com.jira.dashboard.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;
    private UUID projectId;
    private Boolean isShared;
    private Boolean isSystem;
    private Boolean isFavorite;
    private String layout;
    private String permissionLevel;
    private String sharePermissionType;
    private Integer popularity;
    private Integer ordering;
    private String config;
    private List<DashboardShareResponse> shares;
    private List<GadgetInstanceResponse> gadgets;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID updatedBy;
}