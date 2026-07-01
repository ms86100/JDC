package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "issue_type_schemes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueTypeSchemeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "default_issue_type")
    private String defaultIssueType;

    @Column(name = "issue_type_ids", columnDefinition = "TEXT")
    private String issueTypeIds;

    private Integer projectCount;

    @Column(name = "is_default")
    private Boolean isDefault = false;
}