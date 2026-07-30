package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedStepVersionResponse {

    private UUID id;
    private UUID sharedStepId;
    private Integer versionNumber;
    private List<SharedStepDto> steps;
    private String changeSummary;
    private UUID createdBy;
    private Boolean isCurrent;
    private LocalDateTime createdAt;
}