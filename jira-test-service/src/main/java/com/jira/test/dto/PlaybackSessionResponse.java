package com.jira.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaybackSessionResponse {
    private UUID sessionId;
    private UUID executionId;
    private String name;
    private Integer playbackPositionMs;
    private Boolean isPlaying;
    private Double playbackSpeed;
    private UUID createdBy;
    private LocalDateTime sessionStart;
    private LocalDateTime sessionEnd;
    private String status;
    private Integer eventCount;
}