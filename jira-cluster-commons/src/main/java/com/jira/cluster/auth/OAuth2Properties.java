package com.jira.cluster.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "cluster.oauth2")
public class OAuth2Properties {
    private boolean enabled = false;
    private List<String> trustedIssuers = new ArrayList<>();
    private String jwkSetUri = "";
    private String userNameClaim = "preferred_username";
    private String rolesClaim = "roles";
    private boolean localAuthFallback = true;
}
