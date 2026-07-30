package com.avionics_systems.plan.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarningResponse {

    private UUID id;
    private UUID planId;
    private UUID issueId;
    private String issueKey;
    private String issueSummary;
    private String warningType;
    private String message;
    private String severity;
    private Boolean isActive;
    private LocalDateTime dismissedAt;
    private LocalDateTime createdAt;
}
