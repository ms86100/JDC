package com.avionics_systems.migration.jiradc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraDcConnectionConfig {

    private String baseUrl;
    private String pat;
    private List<String> projectKeys;
    private String jqlFilter;

    @Builder.Default
    private int maxResults = 100;
    @Builder.Default
    private int maxConcurrentRequests = 5;
    @Builder.Default
    private int connectTimeoutMs = 10000;
    @Builder.Default
    private int readTimeoutMs = 60000;
    @Builder.Default
    private int retryAttempts = 3;
    @Builder.Default
    private long retryDelayMs = 1000;

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
}
