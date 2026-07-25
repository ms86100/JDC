package com.jira.cluster.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "cluster.oauth2.enabled", havingValue = "true")
@EnableConfigurationProperties(OAuth2Properties.class)
public class OAuth2AutoConfiguration {
}
