package com.avionics_systems.user.dto;

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
public class GroupResponse {
    private UUID id;
    private String name;
    private String description;
    private boolean active;
    private LocalDateTime createdDate;
    private boolean isSystem;
    private int userCount;
    private List<SchemeInfo> permissionSchemes;
    private List<SchemeInfo> notificationSchemes;
    private List<SchemeInfo> securitySchemes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchemeInfo {
        private UUID id;
        private String name;
    }
}