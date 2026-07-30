package com.avionics_systems.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "change_card_metadata", schema = "jira_issue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeCardMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "issue_id", nullable = false, unique = true)
    private UUID issueId;

    @Column(name = "change_type", length = 20)
    private String changeType;

    @Column(name = "classification", length = 50)
    private String classification;

    @Column(name = "parent_design_item_id")
    private UUID parentDesignItemId;

    @Column(name = "tab_layout_key", length = 50)
    @Builder.Default
    private String tabLayoutKey = "STANDARD";

    @Column(name = "closure_rationale", columnDefinition = "TEXT")
    private String closureRationale;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
