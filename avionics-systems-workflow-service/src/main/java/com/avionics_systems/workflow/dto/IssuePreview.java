package com.avionics_systems.workflow.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssuePreview {
    private UUID issueId;
    private String issueKey;
    private String summary;
    private UUID currentStatusId;
}