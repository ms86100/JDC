package com.avionics_systems.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExploratorySessionResponse {

    private UUID id;
    private UUID projectId;
    private String charter;
    private String charterGoal;
    private String sessionType;
    private Integer timeBoxMinutes;
    private Integer actualDurationMinutes;
    private String status;
    private UUID testerId;
    private String environment;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String notes;
    private List<String> bugs;
    private List<String> ideas;
    private List<String> questions;
    private List<String> evidenceLinks;
    private List<String> defectKeys;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
