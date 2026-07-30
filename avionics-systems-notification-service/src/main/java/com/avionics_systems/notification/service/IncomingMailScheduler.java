package com.avionics_systems.notification.service;

import com.avionics_systems.notification.entity.IncomingMailHandler;
import com.avionics_systems.notification.repository.IncomingMailHandlerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncomingMailScheduler {

    private final IncomingMailHandlerRepository handlerRepository;
    private final IncomingMailService incomingMailService;

    @Scheduled(fixedDelay = 60000)
    @SchedulerLock(name = "IncomingMailScheduler_pollMailboxes", lockAtMostFor = "PT48S", lockAtLeastFor = "PT24S")
    public void pollMailboxes() {
        List<IncomingMailHandler> handlers = handlerRepository.findAllEnabled();

        if (handlers.isEmpty()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();

        for (IncomingMailHandler handler : handlers) {
            try {
                if (shouldPoll(handler, now)) {
                    incomingMailService.processMailbox(handler);
                }
            } catch (Exception e) {
                log.error("Error polling mailbox for handler {}: {}", handler.getId(), e.getMessage());
            }
        }
    }

    private boolean shouldPoll(IncomingMailHandler handler, OffsetDateTime now) {
        if (handler.getLastPollAt() == null) {
            return true;
        }

        OffsetDateTime nextPollTime = handler.getLastPollAt()
                .plusMinutes(handler.getPollIntervalMinutes());

        return now.isAfter(nextPollTime) || now.isEqual(nextPollTime);
    }
}
