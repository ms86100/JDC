package com.jira.test.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AdminServiceClientConfig {

    @Value("${admin.service.url:http://localhost:8093}")
    private String adminServiceUrl;

    @Bean
    public RestTemplate adminRestTemplate() {
        return new RestTemplate();
    }

    public String getAdminServiceUrl() {
        return adminServiceUrl;
    }
}
