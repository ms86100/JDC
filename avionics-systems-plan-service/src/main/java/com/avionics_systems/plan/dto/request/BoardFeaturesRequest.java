package com.avionics_systems.plan.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardFeaturesRequest {
    private Boolean sprints;
    private Boolean backlog;
    private Boolean estimation;
    private Boolean parallelSprints;
}
