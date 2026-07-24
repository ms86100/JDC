package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "backups", schema = "jira_admin")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String filename;

    @Column(name = "file_size")
    private Long fileSize;

    @Builder.Default
    private String status = "PENDING";

    @Column(name = "initiated_by")
    private UUID initiatedBy;

    @Column(name = "started_at")
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
