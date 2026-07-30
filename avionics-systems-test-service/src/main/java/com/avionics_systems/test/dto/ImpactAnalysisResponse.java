package com.avionics_systems.test.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImpactAnalysisResponse {

    private UUID id;
    private UUID projectId;
    private String triggerType;
    private UUID triggerId;
    private List<TestImpactDto> affectedTests;
    private List<String> suggestedSuites;
    private BigDecimal riskScore;
    private BigDecimal confidenceScore;
    private String analyzedBy;
    private LocalDateTime createdAt;
}