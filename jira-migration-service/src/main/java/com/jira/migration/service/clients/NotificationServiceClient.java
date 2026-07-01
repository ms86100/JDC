package com.jira.migration.service.clients;

import com.jira.migration.service.clients.dto.*;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

/**
 * Service client for the Notification Service.
 * Provides operations for sending and managing notifications.
 */
@Service
@Slf4j
public class NotificationServiceClient extends BaseServiceClient {

    private static final String SERVICE_NAME = "notificationService";
    private static final String SERVICE_PATH = "/api/notifications";
    private static final String USER_NOTIFICATIONS_PATH = "/api/notifications/user/{userId}";

    @Autowired
    public NotificationServiceClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${services.notificationServiceUrl:http://localhost:8087}") String baseUrl) {
        super(restTemplate, objectMapper, circuitBreakerRegistry, SERVICE_NAME, baseUrl);
    }

    @Override
    protected String getCircuitBreakerName() {
        return "notificationService";
    }

    @Override
    protected String getServicePathPrefix() {
        return SERVICE_PATH;
    }

    /**
     * Creates and sends a notification.
     *
     * @param request the notification request
     * @return the notification response
     */
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        log.info("Creating notification for recipient: {} (type: {})",
                request.getRecipientId(), request.getNotificationType());
        return executePost(SERVICE_PATH, request, NotificationResponse.class);
    }

    /**
     * Retrieves all notifications for a user.
     *
     * @param userId the user ID
     * @return list of notification responses
     */
    public List<NotificationResponse> getUserNotifications(String userId) {
        log.debug("Fetching notifications for user: {}", userId);

        ParameterizedTypeReference<List<NotificationResponse>> typeRef =
            new ParameterizedTypeReference<List<NotificationResponse>>() {};

        String endpoint = USER_NOTIFICATIONS_PATH.replace("{userId}", userId);
        String url = buildUrl(endpoint);
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<List<NotificationResponse>> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, typeRef);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("User notifications {} -> {} ({}ms), found {} notifications",
                    url, response.getStatusCode(), elapsed,
                    response.getBody() != null ? response.getBody().size() : 0);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("User notifications {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Retrieves unread notifications for a user.
     *
     * @param userId the user ID
     * @return list of unread notification responses
     */
    public List<NotificationResponse> getUnreadUserNotifications(String userId) {
        log.debug("Fetching unread notifications for user: {}", userId);

        ParameterizedTypeReference<List<NotificationResponse>> typeRef =
            new ParameterizedTypeReference<List<NotificationResponse>>() {};

        String endpoint = USER_NOTIFICATIONS_PATH.replace("{userId}", userId) + "/unread";
        String url = buildUrl(endpoint);
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<List<NotificationResponse>> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, typeRef);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Unread user notifications {} -> {} ({}ms), found {} notifications",
                    url, response.getStatusCode(), elapsed,
                    response.getBody() != null ? response.getBody().size() : 0);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Unread user notifications {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Marks a notification as read.
     *
     * @param notificationId the notification ID
     * @return the updated notification response
     */
    public NotificationResponse markAsRead(String notificationId) {
        log.info("Marking notification as read: {}", notificationId);
        String endpoint = SERVICE_PATH + "/" + notificationId + "/read";

        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        String url = buildUrl(endpoint);
        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<NotificationResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, NotificationResponse.class);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Mark as read {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);
            return response.getBody();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Mark as read {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Marks all notifications as read for a user.
     *
     * @param userId the user ID
     */
    public void markAllAsRead(String userId) {
        log.info("Marking all notifications as read for user: {}", userId);
        String endpoint = USER_NOTIFICATIONS_PATH.replace("{userId}", userId) + "/read-all";

        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        String url = buildUrl(endpoint);
        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, Void.class);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Mark all as read {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Mark all as read {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Gets the count of unread notifications for a user.
     *
     * @param userId the user ID
     * @return the unread notification count
     */
    public int getUnreadCount(String userId) {
        log.debug("Getting unread notification count for user: {}", userId);
        String endpoint = USER_NOTIFICATIONS_PATH.replace("{userId}", userId) + "/unread/count";
        String url = buildUrl(endpoint);

        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<NotificationCountResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, NotificationCountResponse.class);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Unread count {} -> {} ({}ms)", url, response.getStatusCode(), elapsed);
            return response.getBody() != null ? response.getBody().getCount() : 0;
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Unread count {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Deletes a notification.
     *
     * @param notificationId the notification ID
     */
    public void deleteNotification(String notificationId) {
        log.info("Deleting notification: {}", notificationId);
        String endpoint = SERVICE_PATH + "/" + notificationId;
        executeDelete(endpoint);
    }

    /**
     * Sends a notification for issue creation.
     *
     * @param issueId the created issue ID
     * @param recipientId the recipient user ID
     * @param subject the notification subject
     */
    public void notifyIssueCreated(String issueId, String recipientId, String subject) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .recipientId(recipientId)
                .notificationType("ISSUE_CREATED")
                .subject(subject)
                .issueId(issueId)
                .inAppNotification(true)
                .emailNotification(true)
                .build();
        createNotification(request);
    }

    /**
     * Sends a notification for issue assignment.
     *
     * @param issueId the issue ID
     * @param assigneeId the assignee user ID
     * @param assigneeEmail the assignee email
     */
    public void notifyIssueAssigned(String issueId, String assigneeId, String assigneeEmail) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .recipientId(assigneeId)
                .notificationType("ISSUE_ASSIGNED")
                .subject("Issue assigned to you")
                .issueId(issueId)
                .inAppNotification(true)
                .emailNotification(true)
                .build();
        createNotification(request);
    }

    /**
     * Sends a notification for comment addition.
     *
     * @param issueId the issue ID
     * @param recipientId the recipient user ID
     * @param commentAuthor the comment author name
     */
    public void notifyCommentAdded(String issueId, String recipientId, String commentAuthor) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .recipientId(recipientId)
                .notificationType("COMMENT_ADDED")
                .subject(commentAuthor + " commented on an issue")
                .issueId(issueId)
                .inAppNotification(true)
                .emailNotification(true)
                .build();
        createNotification(request);
    }

    /**
     * Inner class for notification count response.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NotificationCountResponse {
        private int count;
    }
}