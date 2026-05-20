package com.jira.issue.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class WorkflowTransitionClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${workflow.service.url}")
    private String workflowServiceUrl;

    @SuppressWarnings("unchecked")
    public Map<String, Object> executeTransition(
            UUID issueId,
            UUID projectId,
            UUID userId,
            UUID transitionId,
            UUID statusId,
            String comment,
            UUID resolutionId,
            Map<String, Object> screenInput) {

        Map<String, Object> body = new HashMap<>();
        body.put("issueId", issueId.toString());
        body.put("projectId", projectId != null ? projectId.toString() : null);
        body.put("userId", userId != null ? userId.toString() : null);
        if (transitionId != null) {
            body.put("transitionId", transitionId.toString());
        }
        if (statusId != null) {
            body.put("statusId", statusId.toString());
        }
        if (comment != null) {
            body.put("comment", comment);
        }
        if (resolutionId != null) {
            body.put("resolutionId", resolutionId.toString());
        }
        if (screenInput != null) {
            body.put("screenInput", screenInput);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (userId != null) {
            headers.set("X-User-Id", userId.toString());
        }

        String url = workflowServiceUrl + "/api/workflows/transitions/execute";
        Map<String, Object> response = restTemplate.postForObject(
                url, new HttpEntity<>(body, headers), Map.class);

        if (response == null) {
            throw new IllegalStateException("Empty response from workflow service");
        }
        Object success = response.get("success");
        if (success instanceof Boolean b && !b) {
            String error = response.get("error") != null ? response.get("error").toString() : "Transition failed";
            throw new com.jira.issue.exception.InvalidTransitionException(error);
        }
        return response;
    }
}
