package com.avionics_systems.test.dto;

import com.avionics_systems.test.entity.CoverageThreshold;
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