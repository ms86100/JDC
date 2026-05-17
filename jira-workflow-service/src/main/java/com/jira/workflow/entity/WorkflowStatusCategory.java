package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "workflow_status_categories", schema = "jira_workflow")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStatusCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "category_key", nullable = false, unique = true, length = 50)
    private String categoryKey;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(length = 20)
    private String color;

    @Column
    @Builder.Default
    private Integer sequence = 0;

    @Column(name = "is_system")
    @Builder.Default
    private Boolean isSystem = true;

    public static final String TODO = "TODO";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String DONE = "DONE";
}