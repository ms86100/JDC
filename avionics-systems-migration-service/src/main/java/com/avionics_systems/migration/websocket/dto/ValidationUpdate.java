package com.avionics_systems.migration.websocket.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationUpdate {
    private String jobId;
    private String sessionId;
    private int validatedRows;
    private int totalRows;
    private List<ValidationError> newErrors;
    private boolean complete;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private int row;
        private String field;
        private String message;
        private String errorCode;
        private String severity; // ERROR, WARNING, INFO
    }
}