package com.jira.dashboard.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareDashboardRequest {

    @NotNull(message = "Share type is required")
    private String shareType; // USER, GROUP, PROJECT, ROLE, PUBLIC

    private UUID shareId;

    private String shareName;

    @Builder.Default
    private String permissionType = "VIEW"; // VIEW, EDIT
}