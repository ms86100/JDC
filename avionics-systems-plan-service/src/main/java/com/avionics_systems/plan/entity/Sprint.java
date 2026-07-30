package com.avionics_systems.plan.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sprint entity with full lifecycle management.
 * Mirrors Avionics Systems GreenHopper Sprint implementation.
 */
@Entity
@Table(name = "sprints", schema = "jira_plan", indexes = {
    @Index(name = "idx_sprints_board", columnList = "board_id"),
    @Index(name = "idx_sprints_state", columnList = "state"),
    @Index(name = "idx_sprints_board_state", columnList = "board_id, state"),
    @Index(name = "idx_sprints_start_date", columnList = "start_date")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sprint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private BoardConfig boardConfig;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String goal;  // Sprint goal description

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "complete_date")
    private LocalDateTime completeDate;  // When sprint was closed

    @Column(length = 20)
    @Builder.Default
    private String state = "FUTURE";  // FUTURE, ACTIVE, CLOSED, ABANDONED

    private Integer sequence;

    @Column
    @Builder.Default
    private Integer velocity = 0;

    @Column(name = "wip_limit")
    private Integer wipLimit;  // Work-in-progress limit for sprint

    @Column(name = "committed_points")
    @Builder.Default
    private Integer committedPoints = 0;

    @Column(name = "completed_points")
    @Builder.Default
    private Integer completedPoints = 0;

    @OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SprintIssue> sprintIssues = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return "ACTIVE".equals(state);
    }

    public boolean isClosed() {
        return "CLOSED".equals(state);
    }

    public boolean isFuture() {
        return "FUTURE".equals(state);
    }

    public void start() {
        this.state = "ACTIVE";
        this.startDate = LocalDateTime.now();
    }

    public void close() {
        this.state = "CLOSED";
        this.completeDate = LocalDateTime.now();
    }

    public void abandon() {
        this.state = "ABANDONED";
        this.completeDate = LocalDateTime.now();
    }

    public void reopen() {
        if (!"CLOSED".equals(this.state)) {
            throw new IllegalStateException("Can only reopen a CLOSED sprint");
        }
        this.state = "ACTIVE";
        this.completeDate = null;
    }

    public void addIssue(SprintIssue issue) {
        sprintIssues.add(issue);
        issue.setSprint(this);
    }
}