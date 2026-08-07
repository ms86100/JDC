package com.avionics_systems.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_status_definitions", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStatusDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "status_id", nullable = false, unique = true)
    private UUID statusId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 50)
    private String category;

    @Column(length = 20)
    private String color;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
