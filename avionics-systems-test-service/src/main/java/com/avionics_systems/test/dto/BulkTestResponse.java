package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkTestResponse {
    private int totalRequested;
    private int successCount;
    private int failedCount;
    private List<String> errors;
}
