package com.jira.test.dto;

import com.jira.test.entity.CoverageThreshold;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageThresholdRequest {
    private UUID projectId;
    private UUID requirementId;
    private String requirementKey;
    private BigDecimal minimumCoverage;
    private BigDecimal warningThreshold;
    private Boolean alertEnabled;
}