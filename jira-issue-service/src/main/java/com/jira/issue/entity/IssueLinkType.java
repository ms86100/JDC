package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Issue Link Type - Defines types of links between issues (blocks, relates to, etc.)
 * Matches Jira DC's ISSUELINKTYPE table
 */
@Entity
@Table(name = "issue_link_types", schema = "jira_issue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueLinkType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String inward;  // e.g., "is blocked by"

    @Column(nullable = false, length = 50)
    private String outward;  // e.g., "blocks"

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Predefined link types matching Jira DC
    public static final String BLOCKS = "Blocks";
    public static final String IS_BLOCKED_BY = "Is blocked by";
    public static final String DUPLICATES = "Duplicates";
    public static final String IS_DUPLICATED_BY = "Is duplicated by";
    public static final String RELATES_TO = "Relates to";
    public static final String CAUSES = "Causes";
    public static final String IS_CAUSED_BY = "Is caused by";
    public static final String DEPENDS_ON = "Depends on";
    public static final String IS_DEPENDED_UPON_BY = "Is depended upon by";
    public static final String CLONES = "Clones";
    public static final String IS_CLONED_BY = "Is cloned by";
    public static final String SPLITS_INTO = "Splits into";
    public static final String IS_SPLIT_FROM = "Is split from";
    public static final String SUPERCEEDES = "Supercedes";
    public static final String IS_SUPERCEDED_BY = "Is superseded by";
}
