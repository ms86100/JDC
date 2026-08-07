package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExploratorySessionRequest {

    @NotNull
    private UUID projectId;

    private String charter;
    private String charterGoal;
    private String sessionType;
    private Integer timeBoxMinutes;
    private UUID testerId;
    private String environment;
    private String notes;
    private List<String> bugs;
    private List<String> ideas;
    private List<String> questions;
    private List<String> evidenceLinks;
    private List<String> defectKeys;
}
