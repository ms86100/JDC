package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "dataset_variables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetVariable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "dataset_id", nullable = false)
    private UUID datasetId;

    @Column(name = "variable_name", nullable = false, length = 255)
    private String variableName;

    @Column(name = "variable_type", nullable = false, length = 50)
    @Builder.Default
    private String variableType = "STRING"; // STRING, NUMBER, BOOLEAN, SECRET

    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = true;

    @Column(name = "default_value", length = 1000)
    private String defaultValue;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}