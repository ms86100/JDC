package com.jira.workflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "jira.scripting")
@Getter
@Setter
public class ScriptEngineProperties {

    private boolean enabled = true;
    private long timeoutMs = 5000;
    private int memoryLimitMb = 64;
    private long maxStatements = 500_000;
    private long consoleTimeoutMs = 10_000;
    private int logRetentionDays = 30;

    private List<String> httpWhitelistDomains = new ArrayList<>();
    private long httpTimeoutMs = 5000;
    private List<String> envWhitelistKeys = new ArrayList<>();

    private boolean scheduledEnabled = false;
    private long scheduledPollIntervalMs = 30_000;
}
