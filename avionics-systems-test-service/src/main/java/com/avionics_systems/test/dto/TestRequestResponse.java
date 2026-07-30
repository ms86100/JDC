package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRequestResponse {

    private UUID id;
    private UUID projectId;
    private String issueKey;
    private String summary;
    private String description;
    private String requestType;
    private String status;
    private UUID fixVersionId;
    private UUID assigneeId;
    private Boolean frozen;
    private List<String> labels;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
