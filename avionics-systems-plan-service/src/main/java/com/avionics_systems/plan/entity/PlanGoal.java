package com.avionics_systems.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "plan_goals", schema = "jira_plan", indexes = {
        @Index(name = "idx_plan_goals_plan_id", columnList = "plan_id"),
        @Index(name = "idx_plan_goals_parent", columnList = "parent_goal_id"),
        @Index(name = "idx_plan_goals_status", columnList = "status")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", insertable = false, updatable = false)
    private Plan plan;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 30, nullable = false)
    @Builder.Default
    private String status = "NOT_STARTED";

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(nullable = false)
    @Builder.Default
    private Integer progress = 0;

    @Column(name = "parent_goal_id")
    private UUID parentGoalId;

    @Column(name = "linked_epic_ids", columnDefinition = "TEXT[]")
    private String[] linkedEpicIds;

    @Column(length = 20)
    private String color;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
