package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Board configuration audit log for tracking board changes.
 */
@Entity
@Table(name = "board_config_audit_log", schema = "jira_plan", indexes = {
    @Index(name = "idx_board_audit_board", columnList = "board_id"),
    @Index(name = "idx_board_audit_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardConfigAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;  // BOARD_CREATED, BOARD_UPDATED, BOARD_DELETED, COLUMN_ADDED, COLUMN_UPDATED, COLUMN_DELETED, COLUMNS_REORDERED, FILTER_ADDED, FILTER_DELETED, SWIMLANE_ADDED, SWIMLANE_DELETED, COLOR_ADDED, COLOR_DELETED, FIELD_ADDED, FIELD_DELETED

    @Column(name = "user_id")
    private UUID userId;

    @Column(columnDefinition = "jsonb")
    private String details;  // Additional event details as JSON

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}