package com.avionics_systems.workflow.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "workflow_screen_fields", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowScreenField {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tab_id", nullable = false)
    private WorkflowScreenTab tab;

    @Column(name = "field_id", nullable = false)
    private String fieldId;

    @Column(name = "field_label")
    private String fieldLabel;

    @Column(name = "field_type")
    private String fieldType;

    @Column
    @Builder.Default
    private Boolean required = false;

    @Column
    @Builder.Default
    private Boolean hidden = false;

    @Column
    @Builder.Default
    private Boolean readonly = false;

    @Column(name = "renderertype")
    private String renderertype;

    @Column(name = "order_index")
    @Builder.Default
    private Integer orderIndex = 0;

    @Column(columnDefinition = "TEXT")
    private String config;
}