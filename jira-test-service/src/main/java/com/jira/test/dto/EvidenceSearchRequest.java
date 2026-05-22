package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceSearchRequest {
    private UUID projectId;
    private String query;
    private String evidenceType;
    private String classificationLevel;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
