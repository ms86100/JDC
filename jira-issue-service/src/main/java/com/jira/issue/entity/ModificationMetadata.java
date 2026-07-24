package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "modification_metadata", schema = "jira_issue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModificationMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "issue_id", nullable = false, unique = true)
    private UUID issueId;

    @Column(name = "mod_type", length = 20)
    private String modType;
    // Values: MAJOR, MINOR

    @Column(name = "ata_chapter", length = 50)
    private String ataChapter;

    @Column(name = "certification_impact", columnDefinition = "TEXT")
    private String certificationImpact;

    @Column(name = "mod_rationale", columnDefinition = "TEXT")
    private String modRationale;

    @Column(name = "affected_documents", columnDefinition = "TEXT[]")
    private String[] affectedDocuments;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
