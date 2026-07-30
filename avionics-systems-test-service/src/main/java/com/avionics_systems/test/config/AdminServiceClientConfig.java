package com.avionics_systems.test.config;

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
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(5));
        factory.setReadTimeout(java.time.Duration.ofSeconds(30));
        return new RestTemplate(factory);
    }

    public String getAdminServiceUrl() {
        return adminServiceUrl;
    }
}
