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
public class SprintIssueResponse {
    private UUID id;
    private UUID sprintId;
    private UUID planItemId;
    private UUID issueId;
    private String rankValue;
    private LocalDateTime addedAt;
    private UUID addedBy;
    private LocalDateTime removedAt;
    private String completionStatus;
    private LocalDateTime completedAt;
    private Boolean flagged;
    private String flagReason;
}