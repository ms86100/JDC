package com.avionics_systems.plan.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "initiative_epics", schema = "jira_plan", indexes = {
        @Index(name = "idx_initiative_epic", columnList = "initiative_id, epic_id"),
        @Index(name = "idx_epic_initiative", columnList = "epic_id, initiative_id")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiativeEpic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "initiative_id", nullable = false)
    private UUID initiativeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiative_id", insertable = false, updatable = false)
    private Initiative initiative;

    @Column(name = "epic_id", nullable = false)
    private UUID epicId;

    @Column(name = "epic_key", length = 50)
    private String epicKey;

    @Column(name = "epic_name", length = 255)
    private String epicName;

    @Column(name = "total_story_points")
    @Builder.Default
    private Integer totalStoryPoints = 0;

    @Column(name = "completed_story_points")
    @Builder.Default
    private Integer completedStoryPoints = 0;

    @Column(name = "progress_percentage")
    @Builder.Default
    private Double progressPercentage = 0.0;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "sequence", nullable = false)
    @Builder.Default
    private Integer sequence = 0;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    public void recalculateProgress() {
        if (totalStoryPoints != null && totalStoryPoints > 0) {
            this.progressPercentage = (completedStoryPoints * 100.0) / totalStoryPoints;
        } else {
            this.progressPercentage = 0.0;
        }
    }
}