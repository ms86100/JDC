package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "shared_step_dependencies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parent_shared_step_id", nullable = false)
    private UUID parentSharedStepId;

    @Column(name = "child_shared_step_id", nullable = false)
    private UUID childSharedStepId;

    @Column(name = "dependency_type", length = 50)
    @Builder.Default
    private String dependencyType = "CONTAINS"; // CONTAINS, CALLS, EXTENDS
}