package com.jira.issue.event;

import com.jira.issue.repository.IssueEventOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consumes {@code issue_event_outbox} and forwards to search, notification, and board consumers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IssueEventOutboxPoller {

    private final IssueEventOutboxRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${search.service.url:http://localhost:8088}")
    private String searchServiceUrl;

    @Value("${notification.service.url:http://localhost:8087}")
    private String notificationServiceUrl;

    @Value("${jira.outbox.polling.enabled:true}")
    private boolean pollingEnabled;

    @Scheduled(fixedDelayString = "${jira.outbox.polling.interval-ms:5000}")
    @Transactional
    public void pollAndPublish() {
        if (!pollingEnabled) {
            return;
        }
        List<IssueEventOutbox> batch = repository.findTop50ByPublishedFalseOrderByCreatedAtAsc();
        if (batch.isEmpty()) {
            return;
        }
        for (IssueEventOutbox row : batch) {
            try {
                dispatch(row);
                row.setPublished(true);
                repository.save(row);
            } catch (Exception e) {
                log.warn("Outbox dispatch failed for {}: {}", row.getId(), e.getMessage());
            }
        }
        log.debug("Outbox poller processed {} events", batch.size());
    }

    private void dispatch(IssueEventOutbox row) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", row.getEventType());
        payload.put("issueId", row.getIssueId() != null ? row.getIssueId().toString() : null);
        payload.put("projectId", row.getProjectId() != null ? row.getProjectId().toString() : null);

        notifySearch(payload);
        notifyNotification(row, payload);
    }

    private void notifySearch(Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForObject(
                    searchServiceUrl + "/api/search/index/issue-event",
                    new HttpEntity<>(payload, headers),
                    Map.class);
        } catch (Exception e) {
            log.trace("Search index hook skipped: {}", e.getMessage());
        }
    }

    private void notifyNotification(IssueEventOutbox row, Map<String, Object> payload) {
        if (!shouldNotify(row.getEventType())) {
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>(payload);
            body.put("title", "Issue " + row.getEventType());
            body.put("message", "Issue " + row.getIssueId() + " — " + row.getEventType());
            restTemplate.postForObject(
                    notificationServiceUrl + "/api/notifications/internal/issue-event",
                    new HttpEntity<>(body, headers),
                    Map.class);
        } catch (Exception e) {
            log.trace("Notification hook skipped: {}", e.getMessage());
        }
    }

    private boolean shouldNotify(String eventType) {
        if (eventType == null) {
            return false;
        }
        return eventType.contains("assign")
                || eventType.contains("transition")
                || eventType.contains("comment")
                || eventType.contains("created");
    }
}
