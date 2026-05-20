package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefectLinkResponse {
    private UUID id;
    private String defectKey;
    private UUID executionId;
    private UUID stepResultId;
    private String severity;
    private String status;
    private LocalDateTime createdAt;
}