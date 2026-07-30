package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceSearchResult {
    private List<EvidenceResponse> results;
    private int totalCount;
    private int page;
    private int pageSize;
    private int totalPages;
    private Map<String, Map<String, Long>> facets;
}
