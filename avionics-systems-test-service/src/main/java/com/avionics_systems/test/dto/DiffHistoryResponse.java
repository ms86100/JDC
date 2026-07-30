package com.avionics_systems.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiffHistoryResponse {

    private UUID entityId;
    private UUID comparedWithId;
    private String entityType;
    private String mode; // "evolution" or "difference"
    private List<FieldDiff> diffs;
    private String htmlReport;
    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldDiff {
        private String fieldName;
        private String oldValue;
        private String newValue;
        private String changeType; // ADDED, REMOVED, MODIFIED, UNCHANGED
    }
}
