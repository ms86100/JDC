package com.avionics_systems.plan.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Swimlane configuration (row grouping).
 */
@Entity
@Table(name = "board_swimlanes", schema = "jira_plan", indexes = {
    @Index(name = "idx_board_swimlanes_board", columnList = "board_id")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardSwimlane {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardConfig boardConfig;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "grouping_field", nullable = false, length = 50)
    private String groupingField;  // NONE, EPIC, ASSIGNEE, PROJECT, PRIORITY, LABEL

    @Column
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "collapsed_by_default")
    @Builder.Default
    private Boolean collapsedByDefault = false;

    @Column(nullable = false)
    private Integer sequence;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}