package com.jira.issue.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpicResponse {
    private String id;
    private String name;
    private String summary;
    private String description;
    private String color;
    private String leadId;
    private String leadName;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String linkedIssueId;
    private BigDecimal totalStoryPoints;
    private BigDecimal completedStoryPoints;
    private Integer totalIssueCount;
    private Integer completedIssueCount;
    private BigDecimal progressPercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}