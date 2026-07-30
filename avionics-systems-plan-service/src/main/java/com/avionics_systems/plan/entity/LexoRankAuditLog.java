package com.avionics_systems.plan.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lexorank_audit_log", schema = "jira_plan")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LexoRankAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "operation", nullable = false, length = 20)
    private String operation;

    @Column(name = "old_rank", length = 255)
    private String oldRank;

    @Column(name = "new_rank", length = 255)
    private String newRank;

    @Column(name = "bucket", nullable = false)
    @Builder.Default
    private Integer bucket = 0;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String details;
}