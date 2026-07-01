package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineReviewResponse {

    private UUID quarantineId;
    private UUID testId;
    private String testName;

    private ReviewStatus status;

    private String currentReviewer;
    private LocalDateTime reviewSubmittedAt;
    private LocalDateTime reviewCompletedAt;

    private List<ReviewHistoryEntry> reviewHistory;

    private QuarantineResponse quarantineDetails;

    private Boolean autoRestoreOnFix;
    private Integer estimatedResolutionDays;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewHistoryEntry {
        private LocalDateTime timestamp;
        private UUID reviewerId;
        private String reviewerName;
        private String action;
        private String notes;
        private String previousStatus;
        private String newStatus;
    }

    public enum ReviewStatus {
        pending_review,
        under_review,
        approved_for_restore,
        rejected,
        extended,
        escalated,
        completed
    }
}