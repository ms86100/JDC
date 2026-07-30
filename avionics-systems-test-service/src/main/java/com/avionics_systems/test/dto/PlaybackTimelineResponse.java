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
public class PlaybackTimelineResponse {
    private UUID sessionId;
    private UUID executionId;
    private Integer currentPositionMs;
    private Boolean isPlaying;
    private Double playbackSpeed;
    private Integer totalEvents;
    private Long totalDurationMs;
    private List<TimelineEventResponse> events;
    private List<Map<String, Object>> markers;
    private Map<String, Integer> eventTypes;
    private List<Integer> stepBreaks;
}