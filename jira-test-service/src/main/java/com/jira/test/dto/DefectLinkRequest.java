package com.jira.test.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefectLinkRequest {
    private String defectKey;
    private java.util.UUID executionId;
    private java.util.UUID stepResultId;
    private String severity;
    private String status;
}