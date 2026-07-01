package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TestPlanItem - Many-to-many relationship between TestPlans and TestSets
 */
@Entity
@Table(name = "test_plan_items", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_tpi_plan", columnList = "test_plan_id"),
        @Index(name = "idx_tpi_set", columnList = "test_set_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_plan_id", nullable = false)
    private UUID testPlanId;

    @Column(name = "test_set_id", nullable = false)
    private UUID testSetId;

    @Column(name = "execution_order")
    @Builder.Default
    private Integer executionOrder = 0;

    @Column(name = "added_by")
    private UUID addedBy;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;
}