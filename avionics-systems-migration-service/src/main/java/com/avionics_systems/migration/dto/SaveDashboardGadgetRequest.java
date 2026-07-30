package com.avionics_systems.migration.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveDashboardGadgetRequest {
    private String dashboardKey;
    private String gadgetKey;
    private List<GadgetFieldSelection> fields;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GadgetFieldSelection {
        private String fieldKey;
        private String chartType;
        private Integer displayOrder;
        private Boolean enabled;
    }
}
