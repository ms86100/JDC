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
 * Balancer entry for tracking LexoRank balance state per bucket.
 */
@Entity
@Table(name = "lexorank_balancer", schema = "jira_plan")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LexoRankBalancer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bucket_index", nullable = false, unique = true)
    private Integer bucketIndex;

    @Column(name = "last_rank", length = 255)
    private String lastRank;

    @Column(name = "balance_threshold")
    @Builder.Default
    private Integer balanceThreshold = 5;

    @Column(name = "last_balanced_at")
    private LocalDateTime lastBalancedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}