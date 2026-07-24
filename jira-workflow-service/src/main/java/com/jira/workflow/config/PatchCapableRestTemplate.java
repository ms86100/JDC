package com.jira.workflow.config;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Component
public class PatchCapableRestTemplate {

    private final RestTemplate restTemplate;

    public PatchCapableRestTemplate() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restTemplate = new RestTemplate(factory);
    }

    public RestTemplate get() {
        return restTemplate;
    }
}
