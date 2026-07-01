package com.jira.plan.dto.response;

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
public class EventBurndownResponse {
    private UUID sprintId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<BurndownEvent> events;
    private List<SprintBurndownResponse.BurndownPoint> dailySnapshots;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BurndownEvent {
        private LocalDateTime timestamp;
        private String eventType;
        private UUID planItemId;
        private Integer pointsDelta;
        private Integer oldValue;
        private Integer newValue;
    }
}
