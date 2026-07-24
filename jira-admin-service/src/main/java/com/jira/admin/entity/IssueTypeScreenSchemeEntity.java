package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "issue_type_screen_schemes")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueTypeScreenSchemeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(name = "issue_type_id")
    private String issueTypeId;

    @Column(name = "screen_scheme_id")
    private String screenSchemeId;

    @Column(name = "is_default")
    private Boolean isDefault = false;
}