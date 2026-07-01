package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "plan_items", schema = "jira_plan", indexes = {
        @Index(name = "idx_plan_items_sort", columnList = "plan_id, sort_order")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", insertable = false, updatable = false)
    private Plan plan;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "issue_key", length = 50)
    private String issueKey;

    @Column(name = "issue_title", length = 500)
    private String issueTitle;

    @Column(name = "issue_type", nullable = false, length = 20)
    private String issueType;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "sort_order", nullable = false, length = 255)
    private String sortOrder;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "target_end_date")
    private LocalDate targetEndDate;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "status_category", length = 50)
    private String statusCategory;

    @Column(name = "story_points")
    private Integer storyPoints;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "source_type", length = 20)
    private String sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}