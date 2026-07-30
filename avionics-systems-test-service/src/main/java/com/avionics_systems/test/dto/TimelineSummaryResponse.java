package com.avionics_systems.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineSummaryResponse {
    private UUID executionId;
    private Integer totalEvents;
    private Long totalDurationMs;
    private Map<String, Integer> eventTypes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationSeconds;
}