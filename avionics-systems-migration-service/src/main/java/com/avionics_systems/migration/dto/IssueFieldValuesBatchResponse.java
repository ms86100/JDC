package com.avionics_systems.migration.dto;

import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueFieldValuesBatchResponse {
    /** Issue UUID string keys for JSON serialization */
    private Map<String, List<VisibleFieldResponse>> valuesByIssue;

    public static IssueFieldValuesBatchResponse fromUuidMap(Map<UUID, List<VisibleFieldResponse>> source) {
        Map<String, List<VisibleFieldResponse>> mapped = new java.util.LinkedHashMap<>();
        if (source != null) {
            source.forEach((k, v) -> mapped.put(k.toString(), v));
        }
        return IssueFieldValuesBatchResponse.builder().valuesByIssue(mapped).build();
    }
}
