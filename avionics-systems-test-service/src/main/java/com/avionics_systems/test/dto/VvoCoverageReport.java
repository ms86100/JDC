package com.avionics_systems.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VvoCoverageReport {
    private UUID projectId;
    private UUID fixVersionId;
    private int totalVvos;
    private int coveredVvos;
    private int notCoveredVvos;
    private double coveragePercentage;
    private List<VvoCoverageItem> items;
    private LocalDateTime generatedAt;
}
