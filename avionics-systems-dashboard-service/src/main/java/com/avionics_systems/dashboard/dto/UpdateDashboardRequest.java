package com.avionics_systems.dashboard.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDashboardRequest {

    private String name;
    private String description;
    private Boolean isShared;
    private String layout;
    private String permissionLevel;
    private String config;
    private Integer ordering;
}
