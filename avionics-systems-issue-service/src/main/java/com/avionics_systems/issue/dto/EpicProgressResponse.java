package com.avionics_systems.issue.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpicProgressResponse {
    private String epicId;
    private String epicName;
    private java.math.BigDecimal totalStoryPoints;
    private java.math.BigDecimal completedStoryPoints;
    private java.math.BigDecimal progressPercentage;
    private Integer totalIssueCount;
    private Integer completedIssueCount;
    private Integer remainingIssueCount;
    private java.time.LocalDate estimatedCompletion;
}