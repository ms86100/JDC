package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "evidence_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "step_result_id")
    private UUID stepResultId;

    @Column(name = "evidence_type", nullable = false, length = 50)
    private String evidenceType; // SCREENSHOT, VIDEO, LOG, HAR, PDF, FILE, COMMENT

    @Column(name = "classification_level", length = 50)
    private String classificationLevel; // STEP_LEVEL, RUN_LEVEL, ENVIRONMENT_LEVEL

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(length = 500)
    private String url;

    @Column(length = 500)
    private String thumbnailUrl;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // For inline comments/notes

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata; // Additional key-value metadata

    @Column(name = "retention_policy_id")
    private UUID retentionPolicyId;

    @Column(name = "is_archived")
    @Builder.Default
    private Boolean isArchived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}