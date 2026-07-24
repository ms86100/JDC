package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "field_configuration_items")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldConfigurationItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "field_configuration_id", nullable = false)
    private String fieldConfigurationId;

    @Column(name = "field_key", nullable = false)
    private String fieldKey;

    @Column(name = "is_shown")
    private Boolean isShown = true;

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "is_read_only")
    private Boolean isReadOnly = false;

    @Column(name = "renderer")
    private String renderer;
}