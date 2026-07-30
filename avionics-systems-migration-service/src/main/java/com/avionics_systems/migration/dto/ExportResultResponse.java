package com.avionics_systems.migration.dto;

import lombok.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportResultResponse {
    private UUID jobId;
    private String jobStatus;
    private String filePath;
    private String fileFormat;
    private Long fileSizeBytes;
    private Integer totalEntities;
    private Map<String, Integer> entitiesByType;
    private List<EntityExportSummary> exports;
    private String resultMetadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityExportSummary {
        private String entityType;
        private Integer count;
        private String status;
    }
}