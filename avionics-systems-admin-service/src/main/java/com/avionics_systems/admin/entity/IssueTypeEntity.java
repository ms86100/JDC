package com.avionics_systems.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "issue_types")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "issue_type_key")
    private String issueTypeKey;

    @Column(name = "type_order")
    private Integer typeOrder;

    @Column(name = "is_subtask")
    private Boolean isSubtask = false;

    @Column(name = "is_archived")
    private Boolean isArchived = false;
}