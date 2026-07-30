package com.avionics_systems.migration.dto;

import lombok.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMappingResponse {
    private List<FieldMappingInfo> mappings;
    private List<FieldMappingInfo> unmappedFields;
    private List<FieldMappingInfo> highConfidenceMappings;
    private List<FieldMappingInfo> lowConfidenceMappings;
    private Map<String, String> pluginFieldMappings;
    private double averageConfidence;
    private int totalFields;
    private int mappedFields;
    private int unmappedFieldsCount;
    /** P3-03: type incompatibility warnings for proposed mappings */
    private List<String> typeWarnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldMappingInfo {
        private String sourceKey;
        private String targetKey;
        private String confidence;
        private String strategy;
        private String pluginSource;
    }
}