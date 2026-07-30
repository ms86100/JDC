package com.avionics_systems.plan.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "issue_dependencies", schema = "jira_plan")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", insertable = false, updatable = false)
    private Plan plan;

    @Column(name = "blocking_issue_id", nullable = false)
    private UUID blockingIssueId;

    @Column(name = "blocking_issue_key", length = 50)
    private String blockingIssueKey;

    @Column(name = "blocked_issue_id", nullable = false)
    private UUID blockedIssueId;

    @Column(name = "blocked_issue_key", length = 50)
    private String blockedIssueKey;

    @Column(name = "dependency_type", length = 20)
    @Builder.Default
    private String dependencyType = "BLOCKS";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}