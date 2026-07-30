package com.avionics_systems.migration.workflow.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class WorkflowXmlStatusMapper {

    @Value("${app.workflow.default-status-name:Open}")
    private String defaultStatusName;

    private static final Map<String, UUID> OS_STATUS_TO_PLATFORM = Map.ofEntries(
            Map.entry("10000", UUID.fromString("00000000-0000-0000-0001-000000000009")),
            Map.entry("10001", UUID.fromString("00000000-0000-0000-0001-000000000006")),
            Map.entry("10002", UUID.fromString("00000000-0000-0000-0001-000000000004")),
            Map.entry("10003", UUID.fromString("00000000-0000-0000-0001-000000000003")),
            Map.entry("10004", UUID.fromString("00000000-0000-0000-0001-000000000003")),
            Map.entry("10005", UUID.fromString("00000000-0000-0000-0001-000000000005")),
            Map.entry("10006", UUID.fromString("00000000-0000-0000-0001-000000000008"))
    );

    private static final Map<String, UUID> NAME_TO_PLATFORM = Map.ofEntries(
            Map.entry("draft", UUID.fromString("00000000-0000-0000-0001-000000000009")),
            Map.entry("open", UUID.fromString("00000000-0000-0000-0001-000000000006")),
            Map.entry("cab review", UUID.fromString("00000000-0000-0000-0001-000000000004")),
            Map.entry("in progress", UUID.fromString("00000000-0000-0000-0001-000000000003")),
            Map.entry("deploying", UUID.fromString("00000000-0000-0000-0001-000000000003")),
            Map.entry("done", UUID.fromString("00000000-0000-0000-0001-000000000005")),
            Map.entry("rejected", UUID.fromString("00000000-0000-0000-0001-000000000008")),
            Map.entry("closed", UUID.fromString("00000000-0000-0000-0001-000000000008"))
    );

    public UUID resolvePlatformStatusId(String osStatusId, String statusName) {
        if (osStatusId != null) {
            UUID mapped = OS_STATUS_TO_PLATFORM.get(osStatusId);
            if (mapped != null) {
                return mapped;
            }
        }
        if (statusName != null) {
            UUID byName = NAME_TO_PLATFORM.get(statusName.toLowerCase(Locale.ROOT).trim());
            if (byName != null) {
                return byName;
            }
        }
        return UUID.fromString("00000000-0000-0000-0001-000000000006");
    }

    public String resolveStatusName(String osStatusId, String stepName, Map<String, String> stepMeta) {
        if (stepMeta != null && stepMeta.containsKey("legacy.status.name")) {
            return stepMeta.get("legacy.status.name");
        }
        if (stepName != null) {
            return stepName;
        }
        return osStatusId != null ? osStatusId : defaultStatusName;
    }
}
