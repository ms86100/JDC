package com.avionics_systems.test.dto;

import lombok.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineReviewRequest {

    @NotNull(message = "Quarantine ID is required")
    private UUID quarantineId;

    @NotNull(message = "Review action is required")
    private ReviewAction action;

    private String reviewerNotes;

    private String recommendedAction; // restore, extend, escalate, permanent_quarantine

    private Integer extendDurationDays;

    private Boolean autoRestoreOnFix;

    public enum ReviewAction {
        approve_restore,
        reject_restore,
        extend_quarantine,
        escalate,
        mark_permanent,
        request_more_info
    }
}