package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_quarantine")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestQuarantine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_id", nullable = false, unique = true)
    private UUID testId;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "candidate"; // candidate, quarantined, investigation, restored

    @Column(name = "quarantine_reason", columnDefinition = "TEXT")
    private String quarantineReason;

    @Column(name = "trigger_type", length = 50)
    private String triggerType; // auto_flaky, auto_failing, manual

    @Column(name = "triggered_by")
    private UUID triggeredBy;

    @CreationTimestamp
    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @Column(name = "auto_restore_enabled")
    @Builder.Default
    private Boolean autoRestoreEnabled = true;

    @Column(name = "auto_restore_conditions", columnDefinition = "JSONB")
    private String autoRestoreConditions; // {passCount: 3, daysElapsed: 7}

    @Column(name = "current_execution_count")
    @Builder.Default
    private Integer currentExecutionCount = 0;

    @Column(name = "current_pass_count")
    @Builder.Default
    private Integer currentPassCount = 0;

    @Column(name = "last_execution_at")
    private LocalDateTime lastExecutionAt;

    @Column(name = "last_status", length = 50)
    private String lastStatus;

    @Column(name = "restored_at")
    private LocalDateTime restoredAt;

    @Column(name = "restored_by")
    private UUID restoredBy;

    @Column(name = "restore_reason", columnDefinition = "TEXT")
    private String restoreReason;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}