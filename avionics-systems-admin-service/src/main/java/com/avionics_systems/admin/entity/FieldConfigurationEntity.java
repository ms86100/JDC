package com.avionics_systems.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "field_configurations")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldConfigurationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Transient
    private List<FieldConfigurationItemEntity> items;
}