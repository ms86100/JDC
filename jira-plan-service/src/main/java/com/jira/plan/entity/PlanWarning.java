package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "plan_warnings", schema = "jira_plan")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", insertable = false, updatable = false)
    private Plan plan;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "issue_key", length = 50)
    private String issueKey;

    @Column(name = "warning_type", nullable = false, length = 50)
    private String warningType;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(length = 20)
    @Builder.Default
    private String severity = "WARNING";

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "dismissed_at")
    private LocalDateTime dismissedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}