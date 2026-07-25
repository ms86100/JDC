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

    @NotBlank(message = "{validation.ldap.server.url.required}")
    private String serverUrl;

    @NotBlank(message = "{validation.ldap.base.dn.required}")
    private String baseDn;

    @NotBlank(message = "{validation.ldap.bind.dn.required}")
    private String bindDn;

    private String bindPassword;

    private String userSearchBase;
    private String userSearchFilter;
    private String groupSearchBase;
    private String groupSearchFilter;
    private Integer syncIntervalMinutes;
}
