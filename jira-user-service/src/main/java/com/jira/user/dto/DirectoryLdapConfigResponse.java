package com.jira.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectoryLdapConfigResponse {
    private UUID directoryId;
    private String directoryName;
    private String serverUrl;
    private String baseDn;
    private String bindDn;
    private String userSearchBase;
    private String userSearchFilter;
    private String groupSearchBase;
    private String groupSearchFilter;
    private Integer syncIntervalMinutes;
    private LocalDateTime lastSyncAt;
    private String syncStatus;
}
