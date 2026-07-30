package com.avionics_systems.test.event.listener;

import com.avionics_systems.test.event.TestExecutionCompletedEvent;
import com.avionics_systems.test.event.TestRunUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardRefreshListener {

    private final SimpMessagingTemplate messagingTemplate;

    // In-memory cache for dashboard data (in production, use Redis or similar)
    private final Map<UUID, DashboardCache> dashboardCache = new ConcurrentHashMap<>();

    @Async
    @EventListener
    public void onTestRunUpdated(TestRunUpdatedEvent event) {
        log.info("DashboardRefreshListener: Received TestRunUpdatedEvent for execution: {}",
                event.getExecutionId());
        try {
            refreshDashboardCache(event.getProjectId());
            sendDashboardUpdate(event.getProjectId(), "TEST_RUN_UPDATED");
        } catch (Exception e) {
            log.error("Failed to refresh dashboard for project: {} - {}",
                    event.getProjectId(), e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void onTestExecutionCompleted(TestExecutionCompletedEvent event) {
        log.info("DashboardRefreshListener: Received TestExecutionCompletedEvent for execution: {}",
                event.getExecutionId());
        try {
            refreshDashboardCache(event.getProjectId());
            sendDashboardUpdate(event.getProjectId(), "EXECUTION_COMPLETED");

            // Send WebSocket notification for real-time dashboard update
            Map<String, Object> payload = new HashMap<>();
            payload.put("executionId", event.getExecutionId());
            payload.put("finalStatus", event.getFinalStatus());
            payload.put("passedTests", event.getPassedTests());
            payload.put("failedTests", event.getFailedTests());

            messagingTemplate.convertAndSend(
                    "/topic/dashboard/" + event.getProjectId(),
                    payload
            );
            log.info("Dashboard refresh notification sent for project: {}", event.getProjectId());
        } catch (Exception e) {
            log.error("Failed to refresh dashboard on execution complete: {} - {}",
                    event.getProjectId(), e.getMessage(), e);
        }
    }

    private void refreshDashboardCache(UUID projectId) {
        DashboardCache cache = dashboardCache.computeIfAbsent(projectId, k -> new DashboardCache());
        cache.setLastRefreshed(System.currentTimeMillis());
        log.debug("Dashboard cache refreshed for project: {}", projectId);
    }

    private void sendDashboardUpdate(UUID projectId, String eventType) {
        Map<String, Object> update = new HashMap<>();
        update.put("type", eventType);
        update.put("projectId", projectId);
        update.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/dashboard/" + projectId, update);
    }

    private static class DashboardCache {
        private volatile long lastRefreshed;
        private final Map<String, Object> data = new HashMap<>();

        public long getLastRefreshed() {
            return lastRefreshed;
        }

        public void setLastRefreshed(long lastRefreshed) {
            this.lastRefreshed = lastRefreshed;
        }

        public Map<String, Object> getData() {
            return data;
        }
    }
}