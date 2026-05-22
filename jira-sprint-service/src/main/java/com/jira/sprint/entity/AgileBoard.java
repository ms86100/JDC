package com.jira.sprint.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** @deprecated Legacy duplicate of {@link com.jira.board.entity.AgileBoard}; not JPA-mapped. */
@Deprecated
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgileBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "board_type", nullable = false, length = 50)
    @Builder.Default
    private String boardType = "SCRUM";  // SCRUM, KANBAN, BADGE

    @Column(name = "filter_id")
    private UUID filterId;  // Saved filter for the board

    @Column(name = "jql_query", columnDefinition = "TEXT")
    private String jqlQuery;  // JQL query defining board's issues

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "allow_all_issues", nullable = false)
    @Builder.Default
    private Boolean allowAllIssues = true;

    @Column(name = "is_community", nullable = false)
    @Builder.Default
    private Boolean isCommunity = false;

    @Column(name = "location", length = 255)
    private String location;  // Saved dashboard location

    @Column(name = "can_manage", nullable = false)
    @Builder.Default
    private Boolean canManage = true;

    // Column configuration (JSON stored as string)
    @Column(name = "column_config", columnDefinition = "TEXT")
    private String columnConfig;  // JSON: { "columns": [{ "name": "To Do", "statuses": [...] }] }

    @Column(name = "ranking_config", columnDefinition = "TEXT")
    private String rankingConfig;  // Ranking configuration

    @Column(name = "card_layout", length = 50)
    @Builder.Default
    private String cardLayout = "FULL";  // FULL, COMPACT, MINI

    @Column(name = "estimation_statistic", length = 100)
    private String estimationStatistic;  // e.g., "story_point_1", "issue_count"

    @Column(name = "days_on_board", nullable = false)
    @Builder.Default
    private Integer daysOnBoard = 5;

    @Column(name = "backlog_column", length = 100)
    private String backlogColumn;  // Column that represents backlog

    @Column(name = "last_viewed")
    private LocalDateTime lastViewed;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Board types
    public static final String TYPE_SCRUM = "SCRUM";
    public static final String TYPE_KANBAN = "KANBAN";
    public static final String TYPE_BADGE = "BADGE";

    // Card layouts
    public static final String LAYOUT_FULL = "FULL";
    public static final String LAYOUT_COMPACT = "COMPACT";
    public static final String LAYOUT_MINI = "MINI";

    public boolean isScrum() {
        return TYPE_SCRUM.equals(boardType);
    }

    public boolean isKanban() {
        return TYPE_KANBAN.equals(boardType);
    }
}