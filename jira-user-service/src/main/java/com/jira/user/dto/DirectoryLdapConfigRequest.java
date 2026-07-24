package com.jira.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectoryLdapConfigRequest {

    @NotBlank(message = "Server URL is required")
    private String serverUrl;

    @NotBlank(message = "Base DN is required")
    private String baseDn;

    @NotBlank(message = "Bind DN is required")
    private String bindDn;

    private String bindPassword;

    private String userSearchBase;
    private String userSearchFilter;
    private String groupSearchBase;
    private String groupSearchFilter;
    private Integer syncIntervalMinutes;
}
