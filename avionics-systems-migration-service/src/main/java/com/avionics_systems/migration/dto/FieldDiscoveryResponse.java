package com.avionics_systems.migration.dto;

import lombok.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDiscoveryResponse {
    private List<DiscoveredFieldInfo> discoveredFields;
    private int standardFieldCount;
    private int agileFieldCount;
    private int pluginFieldCount;
    private int unknownFieldCount;
    private Set<String> missingFieldKeys;
    private Map<String, List<String>> fieldGroupings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscoveredFieldInfo {
        private String sourceKey;
        private String normalizedKey;
        private String category;
        private String suggestedType;
        private String suggestedRegion;
        private boolean isKnown;
        private boolean requiresProvisioning;
    }
}