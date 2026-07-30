package com.avionics_systems.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "review_sub_task_metadata", schema = "jira_issue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSubTaskMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "issue_id", nullable = false, unique = true)
    private UUID issueId;

    @Column(name = "parent_system_standard_id")
    private UUID parentSystemStandardId;

    @Column(name = "review_type", length = 50)
    private String reviewType;

    @Column(name = "review_status", length = 20)
    @Builder.Default
    private String reviewStatus = "BACKLOG";

    @Column(name = "baseline_start_date")
    private LocalDate baselineStartDate;

    @Column(name = "baseline_end_date")
    private LocalDate baselineEndDate;

    @Column(name = "follow_up_review_id")
    private UUID followUpReviewId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
