package com.avionics_systems.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "backup_schedules", schema = "jira_admin")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "cron_expression")
    @Builder.Default
    private String cronExpression = "0 0 2 * * ?";

    @Column(name = "retention_days")
    @Builder.Default
    private Integer retentionDays = 30;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
