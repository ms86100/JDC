package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineBatchRequest {

    private BatchAction action;

    private java.util.List<UUID> testIds;

    private UUID projectId;

    private String reason;

    private String targetStatus; // For status updates

    private BatchOptions options;

    public enum BatchAction {
        quarantine,
        restore,
        update_status,
        delete,
        submit_for_review,
        extend_duration
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BatchOptions {
        private Boolean autoRestoreEnabled;
        private Map<String, Object> autoRestoreConditions;
        private Integer extendDays;
        private String reviewNotes;
    }
}