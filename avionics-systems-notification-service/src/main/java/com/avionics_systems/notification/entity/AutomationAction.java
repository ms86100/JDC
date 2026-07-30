package com.avionics_systems.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "automation_actions", schema = "jira_notification")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType;

    @Column(name = "action_config", columnDefinition = "TEXT")
    private String actionConfig;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "order_index")
    @Builder.Default
    private Integer orderIndex = 0;

    @Column(name = "failure_handling", length = 50)
    @Builder.Default
    private String failureHandling = "CONTINUE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}