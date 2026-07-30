package com.avionics_systems.migration.service.clients.dto;

import lombok.*;

import java.util.Map;

/**
 * Response DTO for search index status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexStatusResponse {

    private boolean indexAvailable;
    private long totalDocuments;
    private Map<String, Long> documentsByType;
    private long indexSizeBytes;
    private String lastIndexTime;
    private String indexHealth;
    private int availableShards;
    private int totalShards;
    private boolean success;
    private String errorMessage;
}