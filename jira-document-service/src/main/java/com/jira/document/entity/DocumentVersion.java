package com.jira.document.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_versions", schema = "jira_document",
    indexes = {
        @Index(name = "idx_doc_version_document_id", columnList = "document_id"),
        @Index(name = "idx_doc_version_number", columnList = "version_number")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "change_summary", length = 1000)
    private String changeSummary;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "content_hash", length = 64)
    private String contentHash; // For deduplication
}