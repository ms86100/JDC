package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchImpactAnalysisRequest {
    private UUID projectId;
    private List<UUID> testIds;
    private Integer cascadeDepth;
    private Boolean includeTransitive;
}