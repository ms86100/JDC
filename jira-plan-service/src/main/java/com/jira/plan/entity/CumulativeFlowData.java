package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cumulative_flow_data", schema = "jira_plan")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CumulativeFlowData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "sprint_id")
    private UUID sprintId;

    @Column(name = "column_name", length = 100)
    private String columnName;

    @Column(name = "data_date")
    private LocalDate dataDate;

    @Column(name = "issue_count")
    private Integer issueCount;

    @Column(name = "last_processed_date")
    private LocalDateTime lastProcessedDate;
}