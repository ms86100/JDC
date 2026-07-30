package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceViewerData {

    private UUID executionId;
    private String executionKey;
    private List<EvidenceGroup> evidenceGroups;
    private Integer totalCount;
    private Map<String, Long> countByType;
    private Map<String, Long> countByLevel;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvidenceGroup {
        private String groupKey; // step index, time range, etc.
        private String groupLabel;
        private String evidenceLevel; // STEP_LEVEL, RUN_LEVEL, ENVIRONMENT_LEVEL
        private List<EvidenceResponse> evidences;
    }
}