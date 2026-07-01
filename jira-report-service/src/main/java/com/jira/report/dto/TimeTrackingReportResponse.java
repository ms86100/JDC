package com.jira.report.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeTrackingReportResponse {

    private UUID id;
    private String name;
    private UUID projectId;
    private UUID issueId;
    private UUID userId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long totalTimeSeconds;
    private String formattedTotalTime;
    private String worklogDetails;
    private String breakdown;
    private String reportType;
    private LocalDateTime createdAt;
}