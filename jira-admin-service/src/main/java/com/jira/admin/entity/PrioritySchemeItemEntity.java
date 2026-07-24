package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "priority_scheme_items", schema = "jira_admin")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrioritySchemeItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "scheme_id", nullable = false)
    private String schemeId;

    @Column(name = "priority_id", nullable = false)
    private String priorityId;

    @Builder.Default
    private Integer position = 0;
}
