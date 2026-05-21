package com.jira.migration.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FlywayStatusLogger {

    private final Flyway flyway;

    @EventListener(ApplicationReadyEvent.class)
    public void logSchemaVersion() {
        MigrationInfoService info = flyway.info();
        MigrationInfo current = info.current();
        int pending = info.pending().length;
        log.info(
                "Flyway schema ready: current={}, pending={}, applied={}",
                current != null ? current.getVersion() + " " + current.getDescription() : "none",
                pending,
                info.applied().length
        );
        if (pending > 0) {
            log.warn("Flyway has {} pending migration(s) — run mvn flyway:migrate", pending);
        }
    }
}
