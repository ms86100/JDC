package com.jira.migration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "migration_file_uploads", schema = "jira_migration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationFileUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "wizard_session_id", nullable = false)
    private UUID wizardSessionId;

    @Column(name = "migration_job_id")
    private UUID migrationJobId;

    @Column(name = "file_name", nullable = false, columnDefinition = "TEXT")
    private String fileName;

    @Column(name = "mime_type", length = 255)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "checksum", length = 128)
    private String checksum;

    @Lob
    @Column(name = "file_content", columnDefinition = "BYTEA")
    private byte[] fileContent;

    @Column(name = "storage_path", columnDefinition = "TEXT")
    private String storagePath;

    @Column(name = "virus_scan_status", length = 30)
    @Builder.Default
    private String virusScanStatus = "PENDING";

    @Column(name = "parse_status", length = 30)
    @Builder.Default
    private String parseStatus = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
