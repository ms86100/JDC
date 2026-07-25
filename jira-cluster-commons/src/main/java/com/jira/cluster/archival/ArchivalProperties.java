package com.jira.cluster.archival;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cluster.archival")
public class ArchivalProperties {
    private boolean enabled = false;
    private int retentionDays = 365;
    private int batchSize = 1000;
    private String archiveSchema = "archive";
}
