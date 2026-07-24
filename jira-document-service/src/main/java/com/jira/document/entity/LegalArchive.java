package com.jira.document.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "legal_archives", schema = "jira_document",
    indexes = {
        @Index(name = "idx_archive_project_id", columnList = "project_id"),
        @Index(name = "idx_archive_status", columnList = "status"),
        @Index(name = "idx_archive_matter_id", columnList = "legal_matter_id")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "legal_matter_id")
    private UUID legalMatterId;

    @Column(name = "matter_reference", length = 100)
    private String matterReference; // Legal matter reference number

    @Column(name = "archive_type", nullable = false, length = 100)
    private String archiveType; // LITIGATION, REGULATORY, CONTRACT, HR, etc.

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, ARCHIVED, DISPOSED, DISPUTED

    @Column(name = "retention_date")
    private LocalDateTime retentionDate; // When content can be disposed

    @Column(name = "disposition_date")
    private LocalDateTime dispositionDate; // When actually disposed

    @Column(name = "disposition_action", length = 100)
    private String dispositionAction; // DELETE, TRANSFER, ANONYMIZE

    @Column(name = "legal_basis", length = 255)
    private String legalBasis; // GDPR Article basis for retention

    @Column(columnDefinition = "TEXT")
    private String reason; // Reason for archiving

    @Column(name = "archived_by", nullable = false)
    private UUID archivedBy;

    @Column(name = "related_document_ids", columnDefinition = "uuid[]")
    @Builder.Default
    private UUID[] relatedDocumentIds = new UUID[]{};

    @Column(name = "related_issue_ids", columnDefinition = "uuid[]")
    @Builder.Default
    private UUID[] relatedIssueIds = new UUID[]{};

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON metadata

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "review_date")
    private LocalDateTime reviewDate; // Next review date
}