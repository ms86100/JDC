package com.jira.test.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public abstract class TestEvent extends ApplicationEvent {
    private final UUID eventId;
    private final LocalDateTime occurredAt;
    private final UUID projectId;

    protected TestEvent(Object source, UUID projectId) {
        super(source);
        this.eventId = UUID.randomUUID();
        this.occurredAt = LocalDateTime.now();
        this.projectId = projectId;
    }
}