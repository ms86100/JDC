package com.avionics_systems.plan.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacklogResponse {

    private UUID planId;
    private String planName;
    private Integer totalItems;
    private Integer epicCount;
    private Integer storyCount;
    private Integer subtaskCount;
    private List<PlanItemResponse> items;
}
