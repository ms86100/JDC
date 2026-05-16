package com.jira.plan.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;
    private String ownerName;
    private Map<String, Object> settings;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private Long version;
    private Integer itemCount;
    private Integer teamCount;
    private Integer releaseCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
