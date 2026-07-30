package com.avionics_systems.plan.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "initiative_plans", schema = "jira_plan", indexes = {
        @Index(name = "idx_initiative_plan", columnList = "initiative_id, plan_id")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiativePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "initiative_id", nullable = false)
    private UUID initiativeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiative_id", insertable = false, updatable = false)
    private Initiative initiative;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", insertable = false, updatable = false)
    private Plan plan;

    @Column(name = "sequence", nullable = false)
    @Builder.Default
    private Integer sequence = 0;

    @Column(name = "added_at", nullable = false)
    @Builder.Default
    private LocalDateTime addedAt = LocalDateTime.now();
}