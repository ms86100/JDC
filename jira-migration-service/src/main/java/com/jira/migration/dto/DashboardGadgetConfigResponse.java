package com.jira.migration.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardGadgetConfigResponse {
    private String dashboardKey;
    private String gadgetKey;
    private List<GadgetFieldDto> configuredFields;
    private List<GadgetEligibleFieldDto> eligibleFields;
    private Map<String, Object> statistics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GadgetFieldDto {
        private String fieldKey;
        private String displayName;
        private String chartType;
        private int displayOrder;
        private boolean enabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GadgetEligibleFieldDto {
        private String fieldKey;
        private String displayName;
        private String fieldType;
        private boolean supportsChart;
        private boolean supportsFilter;
    }
}
