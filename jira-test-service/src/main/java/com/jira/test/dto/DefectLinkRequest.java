package com.jira.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefectLinkRequest {
    private UUID projectId;
    private String defectKey;
    private UUID executionId;
    private UUID stepResultId;
    private String severity;
    private String status;
}