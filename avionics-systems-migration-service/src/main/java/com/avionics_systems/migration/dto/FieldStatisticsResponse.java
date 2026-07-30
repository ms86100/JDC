package com.avionics_systems.migration.dto;

import lombok.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldStatisticsResponse {
    private UUID fieldDefinitionId;
    private String fieldKey;
    private String fieldType;
    private long totalIssues;
    private long issuesWithValues;
    private long nullValueCount;
    private long uniqueValueCount;
    private Map<String, Long> valueDistribution;
}