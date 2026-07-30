package com.avionics_systems.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineEventResponse {
    private UUID id;
    private String eventType;
    private LocalDateTime eventTimestamp;
    private Integer stepIndex;
    private String eventData;
    private String screenshotPath;
    private List<String> logEntries;
    private Integer sequenceOrder;
    private Map<String, Object> metadata;
}