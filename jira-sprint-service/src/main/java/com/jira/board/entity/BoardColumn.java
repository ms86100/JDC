package com.jira.board.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "board_columns")
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
    private int sequence;

    @Column(name = "status_category", length = 50)
    @Builder.Default
    private String statusCategory = "TODO";

    @Column(name = "is_done", nullable = false)
    @Builder.Default
    private Boolean isDone = false;

    @Column(name = "max_issues")
    private Integer maxIssues;

    @Column(length = 20)
    @Builder.Default
    private String color = "#6c757d";

    @Column(name = "is_collapsible", nullable = false)
    @Builder.Default
    private Boolean isCollapsible = true;

    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean isHidden = false;

    public static final String CATEGORY_TODO = "TODO";
    public static final String CATEGORY_IN_PROGRESS = "IN_PROGRESS";
    public static final String CATEGORY_IN_REVIEW = "IN_REVIEW";
    public static final String CATEGORY_DONE = "DONE";
}