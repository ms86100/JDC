package com.avionics_systems.sprint.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/** @deprecated Legacy duplicate of {@link com.avionics_systems.board.entity.BoardColumn}; not JPA-mapped. */
@Deprecated
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private Integer sequence = 0;

    @Column(name = "status_ids", columnDefinition = "TEXT")
    private String statusIds;  // JSON array of status IDs

    @Column(name = "status_category", length = 50)
    private String statusCategory;  // TODO, IN_PROGRESS, DONE

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDone = false;  // Column is "done" category

    @Column(name = "max_issues")
    private Integer maxIssues;  // WIP limit

    @Column(name = "color", length = 7)
    private String color;  // Hex color for the column

    @Column(name = "is_collapsible", nullable = false)
    @Builder.Default
    private Boolean isCollapsible = true;

    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean isHidden = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Status categories (Avionics Systems DC compatible)
    public static final String CATEGORY_TODO = "TODO";
    public static final String CATEGORY_IN_PROGRESS = "IN_PROGRESS";
    public static final String CATEGORY_DONE = "DONE";
}