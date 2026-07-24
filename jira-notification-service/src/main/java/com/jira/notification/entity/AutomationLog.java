package com.jira.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "automation_logs", schema = "jira_notification")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(name = "trigger_type", nullable = false, length = 100)
    private String triggerType;

    @Column(name = "trigger_event_id")
    private UUID triggerEventId;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "conditions_evaluated")
    private Integer conditionsEvaluated;

    @Column(name = "conditions_passed")
    private Integer conditionsPassed;

    @Column(name = "actions_executed")
    private Integer actionsExecuted;

    @Column(name = "actions_failed")
    private Integer actionsFailed;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    @Column(name = "context_data", columnDefinition = "TEXT")
    private String contextData;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}