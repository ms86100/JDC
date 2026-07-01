package com.jira.plan.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseResponse {

    private UUID id;
    private UUID planId;
    private String name;
    private String version;
    private String description;
    private LocalDate releaseDate;
    private String status;
    private UUID approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private Integer itemCount;
    private Integer completedCount;
    private Double progress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
}
