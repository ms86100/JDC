package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quarantine_transitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "quarantine_id", nullable = false)
    private UUID quarantineId;

    @Column(name = "from_status", length = 50)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 50)
    private String toStatus;

    @Column(name = "transition_reason", columnDefinition = "TEXT")
    private String transitionReason;

    @Column(name = "transitioned_by")
    private UUID transitionedBy;

    @CreationTimestamp
    @Column(name = "transitioned_at")
    private LocalDateTime transitionedAt;

    // Add field to match builder usage in QuarantineService
    @Column(name = "triggered_by")
    private UUID triggeredByField;

    @Column(name = "trigger_type")
    private String triggerType;

    @Column(columnDefinition = "TEXT")
    private String notes;
}