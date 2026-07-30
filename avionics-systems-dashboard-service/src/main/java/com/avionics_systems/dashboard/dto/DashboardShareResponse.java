package com.avionics_systems.dashboard.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardShareResponse {

    private UUID id;
    private UUID dashboardId;
    private String shareType;
    private UUID shareId;
    private String shareName;
    private String permissionType;
    private LocalDateTime createdAt;
}
