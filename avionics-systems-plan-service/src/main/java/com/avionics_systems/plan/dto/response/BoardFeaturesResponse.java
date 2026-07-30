package com.avionics_systems.plan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardFeaturesResponse {
    private UUID boardId;
    private boolean sprints;
    private boolean backlog;
    private boolean estimation;
    private boolean parallelSprints;
}
