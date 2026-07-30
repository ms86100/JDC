package com.avionics_systems.plan.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sprint_goal_history", schema = "jira_plan")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SprintGoalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sprint_id", nullable = false)
    private UUID sprintId;

    @Column(name = "old_goal", columnDefinition = "TEXT")
    private String oldGoal;

    @Column(name = "new_goal", columnDefinition = "TEXT")
    private String newGoal;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "change_reason")
    private String changeReason;
}