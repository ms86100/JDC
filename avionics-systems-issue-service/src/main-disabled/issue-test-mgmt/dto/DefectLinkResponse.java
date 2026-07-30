package com.avionics_systems.issue.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for defect link
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefectLinkResponse {

    private UUID id;
    private String defectKey;
    private String defectSummary;
    private String defectType;
    private UUID testExecutionId;
    private UUID stepResultId;
    private UUID testIssueId;
    private String severity;
    private String status;
    private String priority;
    private LocalDateTime linkedAt;
}