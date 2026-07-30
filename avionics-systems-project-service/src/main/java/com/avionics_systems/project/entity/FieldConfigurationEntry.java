package com.avionics_systems.project.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "field_configuration_entries", schema = "jira_project")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldConfigurationEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "scheme_id", nullable = false)
    private UUID schemeId;

    @Column(name = "issue_type_id")
    private UUID issueTypeId;

    @Column(name = "field_key", nullable = false, length = 64)
    private String fieldKey;

    @Column(nullable = false)
    @Builder.Default
    private Boolean required = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean visible = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hidden = false;
}
