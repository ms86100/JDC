package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepResultResponse {
    private UUID id;
    private UUID executionId;
    private UUID stepId;
    private String status;
    private String actualResult;
    private List<String> evidenceUrls;
    private String defectKey;
    private String comment;
    private LocalDateTime executedAt;
    private LocalDateTime createdAt;
}