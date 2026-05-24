package com.jira.workflow.config;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate that supports PATCH (default HttpURLConnection on some JDKs rejects PATCH).
 */
@Component
public class PatchCapableRestTemplate {

    private final RestTemplate restTemplate;

    public PatchCapableRestTemplate() {
        this.restTemplate = new RestTemplate(new JdkClientHttpRequestFactory());
    }

    public RestTemplate get() {
        return restTemplate;
    }
}
