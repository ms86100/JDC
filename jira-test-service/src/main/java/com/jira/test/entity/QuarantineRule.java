package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "quarantine_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "rule_name", nullable = false, length = 255)
    private String ruleName;

    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType; // flaky_threshold, failure_streak, environment

    @Column(name = "conditions", columnDefinition = "JSONB", nullable = false)
    private String conditions; // {flakyScore: 0.7, consecutiveFails: 10}

    @Column(name = "auto_quarantine")
    @Builder.Default
    private Boolean autoQuarantine = true;

    @Column(name = "notify_on_trigger")
    @Builder.Default
    private Boolean notifyOnTrigger = true;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
}