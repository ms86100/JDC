package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_component_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestComponentMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_id", nullable = false)
    private UUID testId;

    @Column(name = "component_id", nullable = false)
    private UUID componentId;

    @Column(name = "confidence_score", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal confidenceScore = BigDecimal.ONE;

    @Column(name = "mapping_type", length = 50)
    @Builder.Default
    private String mappingType = "direct"; // direct, indirect, ai-suggested

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}