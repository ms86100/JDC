package com.avionics_systems.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "worklogs", schema = "jira_issue")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Worklog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "time_spent_seconds", nullable = false)
    private Long timeSpentSeconds;

    @Column(name = "time_spent_display", length = 30)
    private String timeSpentDisplay;

    @Column(name = "work_description", columnDefinition = "TEXT")
    private String workDescription;

    @Column(name = "visibility", length = 50)
    private String visibility;

    @Column(name = "visibility_group_id")
    private UUID visibilityGroupId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getFormattedTimeSpent() {
        if (timeSpentSeconds == null) return "";
        long s = timeSpentSeconds;
        long weeks = s / (5 * 8 * 3600);
        s = s % (5 * 8 * 3600);
        long days = s / (8 * 3600);
        s = s % (8 * 3600);
        long hours = s / 3600;
        s = s % 3600;
        long minutes = s / 60;
        StringBuilder sb = new StringBuilder();
        if (weeks > 0) sb.append(weeks).append("w ");
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m");
        return sb.toString().trim();
    }
}