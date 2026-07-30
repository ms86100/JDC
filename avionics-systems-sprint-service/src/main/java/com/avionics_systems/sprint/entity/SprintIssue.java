package com.avionics_systems.sprint.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sprint_issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SprintIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sprint_id", nullable = false)
    private UUID sprintId;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "order_index")
    private Integer orderIndex;

    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private LocalDateTime addedAt;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Column(name = "removed_reason", length = 200)
    private String removedReason;
}