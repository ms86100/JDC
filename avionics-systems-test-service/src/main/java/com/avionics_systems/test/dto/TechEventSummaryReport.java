package com.avionics_systems.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechEventSummaryReport {
    private UUID projectId;
    private int totalEvents;
    private int openCount;
    private int closedCount;
    private int cancelledCount;
    private Map<String, Long> countByStatus;
    private Map<String, Long> countByDefectType;
    private Map<String, Long> countByDefectOrigin;
    private Map<String, Long> countByDefectImpact;
    private LocalDateTime generatedAt;
}
