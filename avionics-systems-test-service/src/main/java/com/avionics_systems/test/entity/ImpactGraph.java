package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "impact_graph", indexes = {
    @Index(name = "idx_graph_source", columnList = "source_type, source_id"),
    @Index(name = "idx_graph_target", columnList = "target_type, target_id"),
    @Index(name = "idx_graph_project", columnList = "project_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImpactGraph {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType; // TEST, COMPONENT, REQUIREMENT

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType; // TEST, COMPONENT, REQUIREMENT

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "impact_type", nullable = false, length = 20)
    @Builder.Default
    private ImpactType impactType = ImpactType.DIRECT;

    @Column(name = "weight", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal weight = BigDecimal.ONE;

    @Column(name = "description")
    private String description;

    @Column(name = "cascade_depth")
    @Builder.Default
    private Integer cascadeDepth = 0;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum ImpactType {
        DIRECT,
        TRANSITIVE,
        CASCADING
    }
}