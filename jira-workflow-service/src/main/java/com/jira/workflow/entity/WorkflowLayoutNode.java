package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "workflow_layout_nodes", schema = "jira_workflow")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowLayoutNode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "layout_id", nullable = false)
    private UUID layoutId;

    @Column(name = "status_id", nullable = false)
    private UUID statusId;

    @Column(name = "node_type", nullable = false, length = 20)
    @Builder.Default
    private String nodeType = "STANDARD";

    @Column(name = "position_x", nullable = false)
    private Double positionX;

    @Column(name = "position_y", nullable = false)
    private Double positionY;

    @Column
    @Builder.Default
    private Double width = 120.0;

    @Column
    @Builder.Default
    private Double height = 60.0;

    @Column(length = 20)
    private String color;

    @Column(name = "is_expanded")
    @Builder.Default
    private Boolean isExpanded = true;

    @Column(length = 100)
    private String label;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    public static final String NODE_TYPE_INITIAL = "INITIAL";
    public static final String NODE_TYPE_STANDARD = "STANDARD";
    public static final String NODE_TYPE_DONE = "DONE";
    public static final String NODE_TYPE_GLOBAL = "GLOBAL";
}