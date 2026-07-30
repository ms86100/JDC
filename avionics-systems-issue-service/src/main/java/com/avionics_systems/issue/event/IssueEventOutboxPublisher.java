package com.avionics_systems.issue.event;

import com.avionics_systems.issue.repository.IssueEventOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class IssueEventOutboxPublisher {

    private final IssueEventOutboxRepository repository;
    private final IssueRealtimeBroadcaster realtimeBroadcaster;

    @Transactional
    public void publish(String eventType, UUID issueId, UUID projectId, String payload) {
        try {
            repository.save(IssueEventOutbox.builder()
                    .eventType(eventType)
                    .issueId(issueId)
                    .projectId(projectId)
                    .payload(payload)
                    .published(false)
                    .build());
        } catch (Exception e) {
            log.warn("Issue outbox unavailable for {} on issue {}: {}", eventType, issueId, e.getMessage());
        }
        try {
            realtimeBroadcaster.publish(eventType, issueId, projectId);
        } catch (Exception e) {
            log.warn("Realtime broadcast failed for {} on issue {}: {}", eventType, issueId, e.getMessage());
        }
        log.debug("Processed issue event {} for issue {}", eventType, issueId);
    }

    @Transactional
    public void publish(String eventType, UUID issueId, UUID projectId) {
        try {
            repository.save(IssueEventOutbox.builder()
                    .eventType(eventType)
                    .issueId(issueId)
                    .projectId(projectId)
                    .payload(null)
                    .published(false)
                    .build());
        } catch (Exception e) {
            log.warn("Issue outbox unavailable for {} on issue {}: {}", eventType, issueId, e.getMessage());
        }
        try {
            realtimeBroadcaster.publish(eventType, issueId, projectId);
        } catch (Exception e) {
            log.warn("Realtime broadcast failed for {} on issue {}: {}", eventType, issueId, e.getMessage());
        }
        log.debug("Processed issue event {} for issue {}", eventType, issueId);
    }
}
