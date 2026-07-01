package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sprint audit log for tracking sprint changes.
 */
@Entity
@Table(name = "sprint_audit_log", schema = "jira_plan", indexes = {
    @Index(name = "idx_sprint_audit_sprint", columnList = "sprint_id"),
    @Index(name = "idx_sprint_audit_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sprint_id", nullable = false)
    private UUID sprintId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;  // CREATED, STARTED, CLOSED, ABANDONED, ISSUE_ADDED, ISSUE_REMOVED, UPDATED

    @Column(name = "user_id")
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}