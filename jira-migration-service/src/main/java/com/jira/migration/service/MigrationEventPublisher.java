package com.jira.migration.service;

import com.jira.migration.entity.MigrationEvent;
import com.jira.migration.repository.MigrationEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationEventPublisher {

    private final MigrationEventRepository eventRepository;

    @Transactional
    public MigrationEvent enqueue(UUID jobId, String eventType, Map<String, Object> payload) {
        return eventRepository.save(MigrationEvent.builder()
                .jobId(jobId)
                .eventType(eventType)
                .payload(payload)
                .status("PENDING")
                .build());
    }

    @Scheduled(fixedDelayString = "${migration.events.publish-interval-ms:15000}")
    @SchedulerLock(name = "MigrationEventPublisher_publishPending", lockAtMostFor = "PT12S", lockAtLeastFor = "PT6S")
    @Transactional
    public void publishPending() {
        for (MigrationEvent event : eventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING")) {
            try {
                dispatch(event);
                event.setStatus("PUBLISHED");
                event.setPublishedAt(LocalDateTime.now());
            } catch (Exception e) {
                event.setStatus("FAILED");
                event.setErrorMessage(e.getMessage());
                log.warn("Event publish failed {}: {}", event.getId(), e.getMessage());
            }
            eventRepository.save(event);
        }
    }

    private void dispatch(MigrationEvent event) {
        log.info("Published migration event job={} type={} payload={}",
                event.getJobId(), event.getEventType(), event.getPayload());
    }
}
