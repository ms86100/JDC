package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "initiatives", schema = "jira_plan", indexes = {
        @Index(name = "idx_initiative_owner", columnList = "owner_id"),
        @Index(name = "idx_initiative_program", columnList = "program_id"),
        @Index(name = "idx_initiative_dates", columnList = "start_date, end_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Initiative {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "program_id")
    private UUID programId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", insertable = false, updatable = false)
    private Program program;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "total_story_points")
    @Builder.Default
    private Integer totalStoryPoints = 0;

    @Column(name = "completed_story_points")
    @Builder.Default
    private Integer completedStoryPoints = 0;

    @Column(name = "progress_percentage")
    @Builder.Default
    private Double progressPercentage = 0.0;

    @Column(length = 7)
    private String color;

    @Column(length = 500)
    private String avatarUrl;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "initiative", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<InitiativePlan> plans = new HashSet<>();

    @OneToMany(mappedBy = "initiative", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<InitiativeEpic> epics = new HashSet<>();

    public void recalculateProgress() {
        if (totalStoryPoints != null && totalStoryPoints > 0) {
            this.progressPercentage = (completedStoryPoints * 100.0) / totalStoryPoints;
        } else {
            this.progressPercentage = 0.0;
        }
    }
}