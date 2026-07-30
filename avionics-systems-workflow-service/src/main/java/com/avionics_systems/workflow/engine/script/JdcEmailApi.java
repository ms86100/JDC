package com.avionics_systems.workflow.engine.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.HostAccess;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class JdcEmailApi {

    private final RestTemplate restTemplate;
    private final String notificationServiceUrl;
    private final AtomicInteger emailCount = new AtomicInteger(0);

    public JdcEmailApi(RestTemplate restTemplate, String notificationServiceUrl) {
        this.restTemplate = restTemplate;
        this.notificationServiceUrl = notificationServiceUrl;
    }

    @HostAccess.Export
    public boolean sendEmail(String to, String subject, String htmlBody) {
        try {
            if (emailCount.incrementAndGet() > 50) { log.warn("Email rate limit exceeded"); return false; }
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
            if (emailCount.incrementAndGet() > 50) { log.warn("Email rate limit exceeded"); return false; }
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

    @HostAccess.Export
    public boolean sendEmailWithAttachment(String to, String subject, String htmlBody, String attachmentName, String attachmentBase64) {
        try {
            if (to == null || subject == null) return false;
            if (emailCount.incrementAndGet() > 50) {
                log.warn("Email rate limit exceeded (50/execution)");
                return false;
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("to", to);
            body.put("subject", subject);
            body.put("htmlBody", htmlBody);
            if (attachmentName != null && attachmentBase64 != null) {
                body.put("attachmentName", attachmentName);
                body.put("attachmentContent", attachmentBase64);
            }
            restTemplate.postForObject(notificationServiceUrl + "/api/notifications/email",
                    new HttpEntity<>(body, headers), Map.class);
            return true;
        } catch (Exception e) {
            log.warn("sendEmailWithAttachment failed: {}", e.getMessage());
            return false;
        }
    }

    @HostAccess.Export
    public boolean sendEmailWithCc(String to, String cc, String bcc, String subject, String body) {
        try {
            if (emailCount.incrementAndGet() > 50) { log.warn("Email rate limit exceeded"); return false; }
            if (to == null || subject == null) return false;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> payload = new HashMap<>();
            payload.put("to", to);
            payload.put("subject", subject);
            payload.put("body", body != null ? body : "");
            payload.put("isHtml", true);
            if (cc != null) payload.put("cc", cc);
            if (bcc != null) payload.put("bcc", bcc);
            restTemplate.postForObject(
                    notificationServiceUrl + "/api/notifications/email",
                    new HttpEntity<>(payload, headers), Map.class);
            return true;
        } catch (Exception e) {
            log.warn("Script email send with cc/bcc failed: {}", e.getMessage());
            return false;
        }
    }

    @HostAccess.Export
    public String getMailBody(Map<String, Object> mailData) {
        try {
            if (mailData == null) return null;
            Object body = mailData.get("body");
            if (body == null) body = mailData.get("htmlBody");
            if (body == null) body = mailData.get("content");
            if (body == null) body = mailData.get("text");
            return body != null ? body.toString() : null;
        } catch (Exception e) {
            log.warn("getMailBody failed: {}", e.getMessage());
            return null;
        }
    }

    @HostAccess.Export
    public String getMailSubject(Map<String, Object> mailData) {
        try {
            if (mailData == null) return null;
            Object subject = mailData.get("subject");
            return subject != null ? subject.toString() : null;
        } catch (Exception e) {
            log.warn("getMailSubject failed: {}", e.getMessage());
            return null;
        }
    }
}
