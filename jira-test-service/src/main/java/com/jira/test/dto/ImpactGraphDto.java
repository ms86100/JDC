package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImpactGraphDto {
    private UUID id;
    private String sourceType;
    private UUID sourceId;
    private String sourceLabel;
    private String targetType;
    private UUID targetId;
    private String targetLabel;
    private String impactType;
    private Double weight;
    private Integer cascadeDepth;
}