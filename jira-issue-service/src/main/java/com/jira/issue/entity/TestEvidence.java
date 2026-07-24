package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TestEvidence - Evidence attachments for test executions
 */
@Entity
@Table(name = "test_evidence", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_te_step", columnList = "step_result_id"),
        @Index(name = "idx_te_execution", columnList = "execution_id"),
        @Index(name = "idx_te_type", columnList = "evidence_type")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "step_result_id")
    private UUID stepResultId;

    @Column(name = "execution_id")
    private UUID executionId;

    @Column(name = "evidence_type", nullable = false, length = 50)
    private String evidenceType; // SCREENSHOT, VIDEO, LOG, REPORT, FILE, COMMENT

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(length = 500)
    private String url; // CDN or storage URL

    @Column(columnDefinition = "TEXT")
    private String content; // For inline comments/notes

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}