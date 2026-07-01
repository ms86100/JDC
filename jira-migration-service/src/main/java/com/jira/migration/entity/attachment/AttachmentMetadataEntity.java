package com.jira.migration.entity.attachment;

import com.jira.migration.dto.attachment.AttachmentMetadata;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * Entity representing attachment metadata stored in the database.
 */
@Entity
@Table(name = "attachment_metadata", schema = "jira_migration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentMetadataEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "issue_id", nullable = false)
    private String issueId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "storage_type", nullable = false)
    private String storageType;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "virus_scan_status")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VirusScanStatus virusScanStatus = VirusScanStatus.NOT_SCANNED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private Map<String, String> metadataJson;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum VirusScanStatus {
        PENDING,
        CLEAN,
        INFECTED,
        ERROR,
        NOT_SCANNED
    }

    /**
     * Convert to DTO.
     */
    public AttachmentMetadata toDto() {
        return AttachmentMetadata.builder()
                .id(this.id)
                .issueId(this.issueId)
                .fileName(this.fileName)
                .originalFileName(this.originalFileName)
                .mimeType(this.mimeType)
                .sizeBytes(this.sizeBytes)
                .storagePath(this.storagePath)
                .storageType(this.storageType)
                .checksum(this.checksum)
                .uploadedBy(this.uploadedBy)
                .uploadedAt(this.uploadedAt)
                .contentHash(this.contentHash)
                .virusScanStatus(convertVirusStatus(this.virusScanStatus))
                .metadata(this.metadataJson)
                .build();
    }

    /**
     * Create from DTO.
     */
    public static AttachmentMetadataEntity fromDto(AttachmentMetadata dto) {
        return AttachmentMetadataEntity.builder()
                .id(dto.getId())
                .issueId(dto.getIssueId())
                .fileName(dto.getFileName())
                .originalFileName(dto.getOriginalFileName())
                .mimeType(dto.getMimeType())
                .sizeBytes(dto.getSizeBytes())
                .storagePath(dto.getStoragePath())
                .storageType(dto.getStorageType())
                .checksum(dto.getChecksum())
                .uploadedBy(dto.getUploadedBy())
                .uploadedAt(dto.getUploadedAt())
                .contentHash(dto.getContentHash())
                .virusScanStatus(convertVirusStatus(dto.getVirusScanStatus()))
                .metadataJson(dto.getMetadata())
                .build();
    }

    private static AttachmentMetadata.VirusScanStatus convertVirusStatus(VirusScanStatus status) {
        if (status == null) return AttachmentMetadata.VirusScanStatus.NOT_SCANNED;
        return switch (status) {
            case PENDING -> AttachmentMetadata.VirusScanStatus.PENDING;
            case CLEAN -> AttachmentMetadata.VirusScanStatus.CLEAN;
            case INFECTED -> AttachmentMetadata.VirusScanStatus.INFECTED;
            case ERROR -> AttachmentMetadata.VirusScanStatus.ERROR;
            case NOT_SCANNED -> AttachmentMetadata.VirusScanStatus.NOT_SCANNED;
        };
    }

    private static VirusScanStatus convertVirusStatus(AttachmentMetadata.VirusScanStatus status) {
        if (status == null) return VirusScanStatus.NOT_SCANNED;
        return switch (status) {
            case PENDING -> VirusScanStatus.PENDING;
            case CLEAN -> VirusScanStatus.CLEAN;
            case INFECTED -> VirusScanStatus.INFECTED;
            case ERROR -> VirusScanStatus.ERROR;
            case NOT_SCANNED -> VirusScanStatus.NOT_SCANNED;
        };
    }
}