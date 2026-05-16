package com.jira.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LdapConfigurationDto {
    private String id;
    private String name;
    private String ldapHost;
    private Integer ldapPort;
    private String baseDn;
    private String userSearchFilter;
    private String groupSearchFilter;
    private String userSearchBase;
    private String groupSearchBase;
    private Boolean isActive;
    private Boolean sslEnabled;
    private Boolean startTlsEnabled;
}