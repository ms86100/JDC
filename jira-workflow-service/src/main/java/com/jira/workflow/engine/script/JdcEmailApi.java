package com.jira.workflow.engine.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.HostAccess;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class JdcEmailApi {

    private final RestTemplate restTemplate;
    private final String notificationServiceUrl;

    public JdcEmailApi(RestTemplate restTemplate, String notificationServiceUrl) {
        this.restTemplate = restTemplate;
        this.notificationServiceUrl = notificationServiceUrl;
    }

    @HostAccess.Export
    public boolean sendEmail(String to, String subject, String htmlBody) {
        try {
            if (to == null || subject == null) return false;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("to", to);
            body.put("subject", subject);
            body.put("body", htmlBody != null ? htmlBody : "");
            body.put("isHtml", true);
            restTemplate.postForObject(
                    notificationServiceUrl + "/api/notifications/email",
                    new HttpEntity<>(body, headers), Map.class);
            return true;
        } catch (Exception e) {
            log.warn("Script email send failed: {}", e.getMessage());
            return false;
        }
    }

    @HostAccess.Export
    public boolean sendToUser(String userId, String subject, String message) {
        try {
            if (userId == null || subject == null) return false;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("subject", subject);
            body.put("message", message != null ? message : "");
            body.put("type", "SCRIPT_NOTIFICATION");
            restTemplate.postForObject(
                    notificationServiceUrl + "/api/notifications",
                    new HttpEntity<>(body, headers), Map.class);
            return true;
        } catch (Exception e) {
            log.warn("Script notification send failed: {}", e.getMessage());
            return false;
        }
    }
}
