package com.avionics_systems.test.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    private final ApplicationEventPublisher eventPublisher;

    @Async("taskExecutor")
    public void publish(TestEvent event) {
        log.info("Publishing event: {} for project: {} with eventId: {}",
                event.getClass().getSimpleName(), event.getProjectId(), event.getEventId());
        eventPublisher.publishEvent(event);
    }

    public void publishSync(TestEvent event) {
        log.info("Publishing event sync: {} for project: {} with eventId: {}",
                event.getClass().getSimpleName(), event.getProjectId(), event.getEventId());
        eventPublisher.publishEvent(event);
    }
}