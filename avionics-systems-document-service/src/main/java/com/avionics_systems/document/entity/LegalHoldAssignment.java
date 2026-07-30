package com.avionics_systems.document.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "legal_hold_assignments", schema = "jira_document",
    indexes = {
        @Index(name = "idx_hold_assignment_hold_id", columnList = "legal_hold_id"),
        @Index(name = "idx_hold_assignment_user_id", columnList = "user_id"),
        @Index(name = "idx_hold_assignment_project_id", columnList = "project_id")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalHoldAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "legal_hold_id", nullable = false)
    private UUID legalHoldId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "issue_id")
    private UUID issueId;

    @Column(name = "assignment_type", nullable = false, length = 50)
    @Builder.Default
    private String assignmentType = "USER"; // USER, PROJECT, ISSUE

    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;

    @Column(name = "acknowledged", nullable = false)
    @Builder.Default
    private Boolean acknowledged = false;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON metadata

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}