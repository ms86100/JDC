package com.avionics_systems.plan.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * LexoRank entry for gap-based ordering.
 * Mirrors the LexoRank implementation for issue ranking.
 */
@Entity
@Table(name = "lexorank_entries", schema = "jira_plan", indexes = {
    @Index(name = "idx_lexorank_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_lexorank_rank", columnList = "rank_value"),
    @Index(name = "idx_lexorank_bucket", columnList = "bucket_id")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LexoRank {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;  // PLAN_ITEM, ISSUE, EPIC

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "bucket_id")
    private Long bucketId;

    @Column(name = "rank_value", nullable = false, length = 255)
    private String rankValue;

    @Column(name = "locked")
    @Builder.Default
    private Boolean locked = false;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "locked_by")
    private UUID lockedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void lock(UUID userId) {
        this.locked = true;
        this.lockedAt = LocalDateTime.now();
        this.lockedBy = userId;
    }

    public void unlock() {
        this.locked = false;
        this.lockedAt = null;
        this.lockedBy = null;
    }
}