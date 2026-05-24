package com.jira.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "automation_triggers", schema = "jira_notification")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationTrigger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(name = "trigger_type", nullable = false, length = 100)
    private String triggerType;

    @Column(name = "trigger_config", columnDefinition = "TEXT")
    private String triggerConfig;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "order_index")
    @Builder.Default
    private Integer orderIndex = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}