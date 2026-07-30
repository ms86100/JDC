package com.avionics_systems.workflow.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "workflow_layout_edges", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowLayoutEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "layout_id", nullable = false)
    private UUID layoutId;

    @Column(name = "transition_id", nullable = false)
    private UUID transitionId;

    @Column(name = "from_node_id")
    private UUID fromNodeId;

    @Column(name = "to_node_id")
    private UUID toNodeId;

    @Column(name = "edge_type", length = 20)
    @Builder.Default
    private String edgeType = "CURVED";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "path_points", columnDefinition = "jsonb")
    private String pathPoints;

    @Column(name = "label_offset_x")
    @Builder.Default
    private Double labelOffsetX = 0.0;

    @Column(name = "label_offset_y")
    @Builder.Default
    private Double labelOffsetY = -15.0;

    @Column(name = "is_looped")
    @Builder.Default
    private Boolean isLooped = false;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    public static final String EDGE_TYPE_STRAIGHT = "STRAIGHT";
    public static final String EDGE_TYPE_CURVED = "CURVED";
    public static final String EDGE_TYPE_ORTHOGONAL = "ORTHOGONAL";
}