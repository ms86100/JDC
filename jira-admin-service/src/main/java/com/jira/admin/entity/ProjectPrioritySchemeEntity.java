package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_priority_schemes", schema = "jira_admin")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectPrioritySchemeEntity {

    @Id
    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "scheme_id", nullable = false)
    private String schemeId;
}
