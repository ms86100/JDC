package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SharedStepUsage - Tracks which tests use which shared steps
 */
@Entity
@Table(name = "shared_step_usage", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_ssu_shared", columnList = "shared_step_id"),
        @Index(name = "idx_ssu_test", columnList = "test_issue_id")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedStepUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "shared_step_id", nullable = false)
    private UUID sharedStepId;

    @Column(name = "test_issue_id", nullable = false)
    private UUID testIssueId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}