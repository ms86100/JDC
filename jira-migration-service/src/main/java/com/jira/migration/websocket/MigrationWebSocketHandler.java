package com.jira.migration.websocket;

import com.jira.migration.config.WebSocketSecurityConfig;
import com.jira.migration.websocket.dto.JobProgressUpdate;
import com.jira.migration.websocket.dto.MigrationError;
import com.jira.migration.websocket.dto.ValidationUpdate;
import com.jira.migration.websocket.dto.ImportCompleteNotification;
import com.jira.migration.websocket.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@RequiredArgsConstructor
@Slf4j
public class MigrationWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSecurityConfig securityConfig;

    // Track active connections: sessionId -> userId
    private final Map<String, String> activeConnections = new ConcurrentHashMap<>();

    // Track job subscriptions: jobId -> Set of sessionIds
    private final Map<String, Set<String>> jobSubscriptions = new ConcurrentHashMap<>();

    // Track user sessions: userId -> Set of sessionIds
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    /**
     * Send progress update to specific user for a specific job
     */
    public void sendProgressUpdate(String jobId, String userId, JobProgressUpdate update) {
        String destination = "/user/" + userId + "/queue/progress";
        WebSocketMessage<JobProgressUpdate> message = WebSocketMessage.of(
                WebSocketMessage.EventType.PROGRESS_UPDATE,
                jobId,
                userId,
                update
        );

        messagingTemplate.convertAndSendToUser(userId, "/queue/progress", message);

        log.debug("Sent progress update to user {} for job {}: {}%",
                userId, jobId, update.getProgressPercentage());
    }

    /**
     * Broadcast progress update to all subscribers of a job
     */
    public void broadcastProgress(String jobId, JobProgressUpdate update) {
        String destination = "/topic/job/" + jobId + "/progress";
        WebSocketMessage<JobProgressUpdate> message = WebSocketMessage.of(
                WebSocketMessage.EventType.PROGRESS_UPDATE,
                jobId,
                null,
                update
        );

        messagingTemplate.convertAndSend(destination, message);

        log.debug("Broadcasted progress for job {}: {}%", jobId, update.getProgressPercentage());
    }

    /**
     * Send validation update to specific user
     */
    public void sendValidationUpdate(String jobId, String userId, ValidationUpdate update) {
        String destination = "/topic/job/" + jobId + "/validation";
        WebSocketMessage<ValidationUpdate> message = WebSocketMessage.of(
                WebSocketMessage.EventType.VALIDATION_ERROR,
                jobId,
                userId,
                update
        );

        messagingTemplate.convertAndSendToUser(userId, "/queue/validation", message);
        messagingTemplate.convertAndSend(destination, message);

        log.debug("Sent validation update for job {}: {} errors", jobId,
                update.getNewErrors() != null ? update.getNewErrors().size() : 0);
    }

    /**
     * Notify job completion to user
     */
    public void sendJobCompleted(String jobId, String userId, ImportCompleteNotification result) {
        String queueDestination = "/user/" + userId + "/queue/completed";
        String topicDestination = "/topic/job/" + jobId + "/completed";

        WebSocketMessage<ImportCompleteNotification> message = WebSocketMessage.of(
                WebSocketMessage.EventType.JOB_COMPLETED,
                jobId,
                userId,
                result
        );

        // Send to user-specific queue
        messagingTemplate.convertAndSendToUser(userId, "/queue/completed", message);

        // Also broadcast to topic
        messagingTemplate.convertAndSend(topicDestination, message);

        log.info("Sent job completion notification for job {} to user {}", jobId, userId);
    }

    /**
     * Send error notification
     */
    public void sendErrorNotification(String jobId, String userId, MigrationError error) {
        String queueDestination = "/user/" + userId + "/queue/errors";

        WebSocketMessage<MigrationError> message = WebSocketMessage.of(
                WebSocketMessage.EventType.ERROR,
                jobId,
                userId,
                error
        );

        messagingTemplate.convertAndSendToUser(userId, "/queue/errors", message);

        // Also broadcast to job topic for monitoring
        messagingTemplate.convertAndSend("/topic/job/" + jobId + "/errors", message);

        log.warn("Sent error notification for job {}: {} - {}",
                jobId, error.getErrorCode(), error.getErrorMessage());
    }

    /**
     * Register a new connection
     */
    public void registerConnection(String sessionId, String userId) {
        activeConnections.put(sessionId, userId);

        // Track user's sessions
        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(sessionId);

        log.info("WebSocket connection registered - session: {}, user: {}", sessionId, userId);
    }

    /**
     * Remove a connection
     */
    public void removeConnection(String sessionId) {
        String userId = activeConnections.remove(sessionId);

        if (userId != null) {
            // Remove from user's sessions
            Set<String> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }

            // Remove from all job subscriptions
            for (Set<String> subscribers : jobSubscriptions.values()) {
                subscribers.remove(sessionId);
            }

            securityConfig.releaseConnection(userId);
            log.info("WebSocket connection removed - session: {}, user: {}", sessionId, userId);
        }
    }

    /**
     * Subscribe a session to a job
     */
    public void subscribeToJob(String sessionId, String jobId) {
        jobSubscriptions.computeIfAbsent(jobId, k -> new CopyOnWriteArraySet<>()).add(sessionId);
        log.debug("Session {} subscribed to job {}", sessionId, jobId);
    }

    /**
     * Unsubscribe a session from a job
     */
    public void unsubscribeFromJob(String sessionId, String jobId) {
        Set<String> subscribers = jobSubscriptions.get(jobId);
        if (subscribers != null) {
            subscribers.remove(sessionId);
            if (subscribers.isEmpty()) {
                jobSubscriptions.remove(jobId);
            }
        }
        log.debug("Session {} unsubscribed from job {}", sessionId, jobId);
    }

    /**
     * Get count of active connections
     */
    public int getActiveConnectionCount() {
        return activeConnections.size();
    }

    /**
     * Get connections for a specific user
     */
    public int getUserConnectionCount(String userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * Get subscriber count for a job
     */
    public int getJobSubscriberCount(String jobId) {
        Set<String> subscribers = jobSubscriptions.get(jobId);
        return subscribers != null ? subscribers.size() : 0;
    }

    /**
     * Send heartbeat to all active connections
     */
    public void sendHeartbeat() {
        for (Map.Entry<String, String> entry : activeConnections.entrySet()) {
            WebSocketMessage<Object> heartbeat = WebSocketMessage.heartbeat(null, entry.getValue());
            messagingTemplate.convertAndSendToUser(entry.getValue(), "/queue/heartbeat", heartbeat);
        }
    }

    /**
     * Send batch completion notification
     */
    public void sendBatchCompleted(String jobId, String userId, int batchNumber, int processedInBatch) {
        Map<String, Object> batchInfo = Map.of(
                "batchNumber", batchNumber,
                "processedCount", processedInBatch,
                "timestamp", Instant.now().toString()
        );

        WebSocketMessage<Map<String, Object>> message = WebSocketMessage.of(
                WebSocketMessage.EventType.BATCH_COMPLETED,
                jobId,
                userId,
                batchInfo
        );

        String topicDestination = "/topic/job/" + jobId + "/batch";
        messagingTemplate.convertAndSend(topicDestination, message);

        log.debug("Sent batch completion for job {}: batch #{}", jobId, batchNumber);
    }
}