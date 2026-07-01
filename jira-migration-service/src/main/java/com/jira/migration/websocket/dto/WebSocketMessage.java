package com.jira.migration.websocket.dto;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage<T> {
    private String eventType;
    private String jobId;
    private String userId;
    private T payload;
    private Instant timestamp;
    private String correlationId;

    public enum EventType {
        PROGRESS_UPDATE,
        VALIDATION_ERROR,
        JOB_COMPLETED,
        JOB_FAILED,
        BATCH_COMPLETED,
        CONNECTION_ESTABLISHED,
        CONNECTION_LOST,
        HEARTBEAT,
        ERROR
    }

    public static <T> WebSocketMessage<T> of(EventType eventType, String jobId, String userId, T payload) {
        return WebSocketMessage.<T>builder()
                .eventType(eventType.name())
                .jobId(jobId)
                .userId(userId)
                .payload(payload)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> WebSocketMessage<T> heartbeat(String jobId, String userId) {
        return of(EventType.HEARTBEAT, jobId, userId, null);
    }

    public static <T> WebSocketMessage<T> error(String jobId, String userId, T error) {
        return of(EventType.ERROR, jobId, userId, error);
    }
}