package com.avionics_systems.project.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "issue_type_scheme_issue_types", schema = "jira_project")
@IdClass(IssueTypeSchemeMapping.IdClass.class)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueTypeSchemeMapping {

    @Id
    @Column(name = "scheme_id")
    private UUID schemeId;

    @Id
    @Column(name = "issue_type_name")
    private String issueTypeName;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdClass implements Serializable {
        private UUID schemeId;
        private String issueTypeName;
    }
}