package com.jira.migration.service.clients.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing a Sprint in the system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SprintDto {

    @EqualsAndHashCode.Include
    private String id;

    private String name;
    private String goal;
    private String projectId;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime completeDate;
    private Integer durationDays;
    private Integer capacity;
    private Integer usedCapacity;
    private List<String> issueIds;
    private Integer issueCount;
    private Integer completedIssueCount;
    private Integer remainingEstimateMinutes;
    private Integer completedEstimateMinutes;
    private boolean archived;
    private LocalDateTime created;
    private LocalDateTime updated;
}