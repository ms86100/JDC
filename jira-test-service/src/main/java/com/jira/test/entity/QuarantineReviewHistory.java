package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quarantine_review_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineReviewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "quarantine_id", nullable = false)
    private UUID quarantineId;

    @Column(name = "action", nullable = false, length = 50)
    private String action; // submitted, assigned, reviewed, decision_made, etc.

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_name")
    private String actorName;

    @Column(name = "actor_type", length = 20)
    private String actorType; // USER, SYSTEM, AUTOMATION

    @Column(name = "previous_status")
    private String previousStatus;

    @Column(name = "new_status")
    private String newStatus;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata; // Additional context

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}