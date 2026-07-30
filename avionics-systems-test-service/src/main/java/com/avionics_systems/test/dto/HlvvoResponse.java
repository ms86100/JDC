package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HlvvoResponse {

    private UUID id;
    private UUID projectId;
    private String issueKey;
    private String summary;
    private String description;
    private String status;
    private LocalDate targetDate;
    private String airbusReference;
    private Integer hlvvoVersion;
    private String proofreadingData;
    private UUID assigneeId;
    private String specificationReference;
    private List<UUID> componentIds;
    private Integer taskProgress;
    private String ptsLink;
    private String mfclLink;
    private UUID fixVersionId;
    private List<String> labels;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
