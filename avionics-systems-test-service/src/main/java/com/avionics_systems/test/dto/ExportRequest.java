package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportRequest {
    @NotBlank(message = "Export format is required")
    private String format; // JSON, CSV, XML, HTML, PDF

    private boolean includeStepDetails = true;
    private boolean includeLogs = false;
    private boolean includeEvidenceLinks = true;
    private boolean includeAnnotations = true;
    private boolean includeTrends = false;

    // For batch export
    private List<UUID> runIds; // If empty, export single run

    private UUID testId; // For exporting all runs of a test
    private UUID projectId; // For exporting all runs of a project
    private Integer days; // For time-based export

    // For filtering in batch exports
    private String status; // Filter by status
    private String environment; // Filter by environment
    private List<String> tags; // Filter by tags
}
