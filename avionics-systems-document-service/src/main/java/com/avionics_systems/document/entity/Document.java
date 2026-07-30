package com.avionics_systems.document.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents", schema = "jira_document")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "issue_id")
    private UUID issueId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "document_type", nullable = false, length = 100)
    private String documentType;

    @Column(length = 255)
    private String space;

    @Column(name = "parent_document_id")
    private UUID parentDocumentId;

    @Column(length = 500)
    private String versionLabel;

    @Column(name = "attachment_url", length = 2000)
    private String attachmentUrl;

    @Column(name = "mime_type", length = 100)
    @Builder.Default
    private String mimeType = "text/html";

    @Column(name = "file_size")
    @Builder.Default
    private Long fileSize = 0L;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @Column(name = "is_archived", nullable = false)
    @Builder.Default
    private Boolean isArchived = false;

    @Column(name = "page_layout", length = 50)
    @Builder.Default
    private String pageLayout = "DEFAULT";

    @Column(name = "labels", columnDefinition = "text[]")
    @Builder.Default
    private String[] labels = new String[]{};

    @Column(name = "external_url", length = 2000)
    private String externalUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "child_position")
    @Builder.Default
    private Integer childPosition = 0;
}
