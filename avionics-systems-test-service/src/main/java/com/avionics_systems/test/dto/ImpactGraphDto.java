package com.avionics_systems.test.dto;

import lombok.*;

import java.math.BigDecimal;
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
    private BigDecimal weight;
    private Integer cascadeDepth;
}