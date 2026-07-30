package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quarantine_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "quarantine_id", nullable = false)
    private UUID quarantineId;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING_REVIEW;

    @Column(name = "current_reviewer")
    private UUID currentReviewer;

    @Column(name = "review_type", length = 50)
    private String reviewType; // auto_review, manual_review, scheduled

    @Column(name = "review_submitted_at")
    private LocalDateTime reviewSubmittedAt;

    @Column(name = "review_started_at")
    private LocalDateTime reviewStartedAt;

    @Column(name = "review_completed_at")
    private LocalDateTime reviewCompletedAt;

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;

    @Column(name = "recommended_action", length = 50)
    private String recommendedAction; // restore, extend, escalate, permanent_quarantine

    @Column(name = "decision_reason", columnDefinition = "TEXT")
    private String decisionReason;

    @Column(name = "estimated_resolution_days")
    private Integer estimatedResolutionDays;

    @Column(name = "auto_restore_on_fix")
    @Builder.Default
    private Boolean autoRestoreOnFix = true;

    @Column(name = "ticket_key", length = 50)
    private String ticketKey;

    @Column(name = "next_review_date")
    private LocalDateTime nextReviewDate;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ReviewStatus {
        PENDING_REVIEW,
        UNDER_REVIEW,
        APPROVED_FOR_RESTORE,
        REJECTED,
        EXTENDED,
        ESCALATED,
        COMPLETED,
        CANCELLED
    }
}