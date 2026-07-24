package com.jira.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.admin.entity.WebhookDeliveryLogEntity;
import com.jira.admin.entity.WebhookEntity;
import com.jira.admin.repository.WebhookDeliveryLogRepository;
import com.jira.admin.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDispatchService {

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryLogRepository deliveryLogRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_RETRY_ATTEMPTS = 3;

    @Transactional(readOnly = true)
    public List<WebhookEntity> getAllWebhooks() {
        return webhookRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<WebhookEntity> getWebhookById(String id) {
        return webhookRepository.findById(id);
    }

    @Transactional
    public WebhookEntity createWebhook(WebhookEntity webhook) {
        return webhookRepository.save(webhook);
    }

    @Transactional
    public Optional<WebhookEntity> updateWebhook(String id, WebhookEntity updates) {
        return webhookRepository.findById(id).map(existing -> {
            existing.setName(updates.getName());
            existing.setUrl(updates.getUrl());
            existing.setSecret(updates.getSecret());
            existing.setEvents(updates.getEvents());
            existing.setIsEnabled(updates.getIsEnabled());
            existing.setJqlFilter(updates.getJqlFilter());
            existing.setExcludeBody(updates.getExcludeBody());
            return webhookRepository.save(existing);
        });
    }

    @Transactional
    public boolean deleteWebhook(String id) {
        if (webhookRepository.existsById(id)) {
            webhookRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<WebhookDeliveryLogEntity> getDeliveryLogs(String webhookId) {
        return deliveryLogRepository.findByWebhookIdOrderByDeliveredAtDesc(webhookId);
    }

    @Transactional
    public void dispatchEvent(String eventType, Map<String, Object> payload) {
        List<WebhookEntity> enabledWebhooks = webhookRepository.findByIsEnabledTrue();

        for (WebhookEntity webhook : enabledWebhooks) {
            if (!matchesEvent(webhook, eventType)) {
                continue;
            }
            deliverWebhook(webhook, eventType, payload);
        }
    }

    @Transactional
    public WebhookDeliveryLogEntity testWebhook(String webhookId) {
        WebhookEntity webhook = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new IllegalArgumentException("Webhook not found: " + webhookId));

        Map<String, Object> testPayload = Map.of(
                "event", "test",
                "timestamp", LocalDateTime.now().toString(),
                "webhookId", webhookId,
                "message", "Test webhook delivery"
        );

        return deliverWebhook(webhook, "test", testPayload);
    }

    private WebhookDeliveryLogEntity deliverWebhook(WebhookEntity webhook, String eventType,
                                                     Map<String, Object> payload) {
        WebhookDeliveryLogEntity deliveryLog = WebhookDeliveryLogEntity.builder()
                .webhookId(webhook.getId())
                .eventType(eventType)
                .deliveryStatus("PENDING")
                .attemptCount(0)
                .deliveredAt(LocalDateTime.now())
                .build();

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
            deliveryLog.setPayload(payloadJson);
        } catch (Exception e) {
            payloadJson = payload.toString();
            deliveryLog.setPayload(payloadJson);
        }

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            deliveryLog.setAttemptCount(attempt);
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Webhook-Event", eventType);
                headers.set("X-Webhook-Id", webhook.getId());

                if (webhook.getSecret() != null && !webhook.getSecret().isEmpty()) {
                    String signature = computeHmacSha256(payloadJson, webhook.getSecret());
                    headers.set("X-Hub-Signature-256", "sha256=" + signature);
                }

                HttpEntity<String> request = new HttpEntity<>(
                        Boolean.TRUE.equals(webhook.getExcludeBody()) ? "{}" : payloadJson,
                        headers
                );

                ResponseEntity<String> response = restTemplate.exchange(
                        webhook.getUrl(), HttpMethod.POST, request, String.class
                );

                deliveryLog.setResponseStatus(response.getStatusCode().value());
                deliveryLog.setResponseBody(truncate(response.getBody(), 2000));
                deliveryLog.setDeliveryStatus("SUCCESS");
                deliveryLog.setDeliveredAt(LocalDateTime.now());
                log.info("Webhook {} delivered to {} (attempt {})", webhook.getId(), webhook.getUrl(), attempt);
                break;
            } catch (Exception e) {
                log.warn("Webhook {} delivery attempt {} failed: {}", webhook.getId(), attempt, e.getMessage());
                deliveryLog.setErrorMessage(e.getMessage());
                deliveryLog.setDeliveryStatus(attempt >= MAX_RETRY_ATTEMPTS ? "FAILED" : "RETRYING");
            }
        }

        return deliveryLogRepository.save(deliveryLog);
    }

    private boolean matchesEvent(WebhookEntity webhook, String eventType) {
        if (webhook.getEvents() == null || webhook.getEvents().isEmpty()) {
            return false;
        }
        String events = webhook.getEvents().toLowerCase();
        return events.contains(eventType.toLowerCase()) || events.contains("*");
    }

    private String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            log.error("Failed to compute HMAC-SHA256: {}", e.getMessage());
            return "";
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
