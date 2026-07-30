package com.avionics_systems.plan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardPermissionResponse {
    private UUID id;
    private UUID boardId;
    private String permissionType;
    private String principalType;
    private UUID principalId;
    private LocalDateTime grantedAt;
    private UUID grantedBy;
}