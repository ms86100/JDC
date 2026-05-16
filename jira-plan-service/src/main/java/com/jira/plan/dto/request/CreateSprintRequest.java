package com.jira.plan.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSprintRequest {
    private String name;
    private String goal;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer wipLimit;
}