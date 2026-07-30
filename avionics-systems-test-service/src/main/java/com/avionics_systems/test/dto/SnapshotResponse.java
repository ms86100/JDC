package com.avionics_systems.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnapshotResponse {
    private UUID snapshotId;
    private UUID sessionId;
    private UUID executionId;
    private String name;
    private String description;
    private Integer eventIndex;
    private Integer positionMs;
    private Double playbackSpeed;
    private LocalDateTime createdAt;
    private UUID createdBy;
    private List<TimelineEventResponse> events;
}