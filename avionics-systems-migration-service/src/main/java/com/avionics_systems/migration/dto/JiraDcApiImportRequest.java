package com.avionics_systems.migration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraDcApiImportRequest {

    @NotBlank(message = "Jira base URL is required")
    private String jiraBaseUrl;

    @NotBlank(message = "Personal Access Token is required")
    private String pat;

    private List<String> projectKeys;
    private String jqlFilter;
    private UUID targetProjectId;

    @Builder.Default
    private int maxResults = 100;
    @Builder.Default
    private boolean includeComments = true;
    @Builder.Default
    private boolean includeAttachments = true;
    @Builder.Default
    private boolean includeWorklogs = true;
    @Builder.Default
    private boolean includeChangelog = false;
    @Builder.Default
    private boolean trustAllCertificates = true;

    private Map<String, Object> options;
}
